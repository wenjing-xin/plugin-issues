# Halo 插件设置表单组件参考

## 概述

Halo 插件设置使用 FormKit Schema 定义表单，与主题设置使用相同的组件系统。

## settings.yaml 基本结构

```yaml
apiVersion: v1alpha1
kind: Setting
metadata:
  name: my-plugin-setting
spec:
  forms:
    - group: basic           # 分组标识
      label: 基本设置         # 分组显示名称
      formSchema:            # 表单字段定义
        - $formkit: text
          name: apiKey
          label: API Key
```

## 在 plugin.yaml 中关联设置

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: Plugin
metadata:
  name: my-plugin
spec:
  settingName: my-plugin-setting      # 关联设置定义
  configMapName: my-plugin-config     # 配置存储名称
  # ...其他配置
```

## 在代码中获取设置

Halo 提供了两个接口用于获取插件配置：

| 接口 | 说明 | 适用场景 |
|-----|------|---------|
| `ReactiveSettingFetcher` | 响应式接口，返回 `Mono` | WebFlux 环境（推荐） |
| `SettingFetcher` | 同步接口，返回 `Optional` | 同步代码场景 |

### ReactiveSettingFetcher 接口

```java
public interface ReactiveSettingFetcher {
    // 获取指定分组配置并转换为 Java 对象
    <T> Mono<T> fetch(String group, Class<T> clazz);
    
    // 获取指定分组的原始 JSON 数据
    Mono<JsonNode> getSettingValue(String group);
    
    // 获取所有分组的配置数据
    Mono<Map<String, JsonNode>> getSettingValues();
}
```

### SettingFetcher 接口（同步）

```java
public interface SettingFetcher {
    // 获取指定分组配置并转换为 Java 对象
    <T> Optional<T> fetch(String group, Class<T> clazz);
    
    // 获取指定分组的原始 JSON 数据
    JsonNode getSettingValue(String group);
    
    // 获取所有分组的配置数据
    Map<String, JsonNode> getSettingValues();
}
```

### 基础用法

```java
@Component
@RequiredArgsConstructor
public class MyService {
    private final ReactiveSettingFetcher settingFetcher;

    public Mono<String> getApiKey() {
        return settingFetcher.fetch("basic", BasicConfig.class)
            .map(BasicConfig::getApiKey);
    }
    
    // 获取原始 JSON 值
    public Mono<String> getSingleValue() {
        return settingFetcher.getSettingValue("basic")
            .map(setting -> setting.get("apiKey").asText());
    }
}

@Data
public class BasicConfig {
    private String apiKey;
    private Integer maxRetries;
    private Boolean enabled;
}
```

### 监听配置变更

当用户修改插件配置时，可以通过监听 `PluginConfigUpdatedEvent` 事件执行相应操作：

```java
@Component
public class SettingChangeListener {

    @EventListener
    public void onConfigUpdated(PluginConfigUpdatedEvent event) {
        // 获取新旧配置
        Map<String, JsonNode> oldValues = event.getOldSettingValues();
        Map<String, JsonNode> newValues = event.getNewSettingValues();
        
        // 检查特定分组是否变更
        if (newValues.containsKey("basic")) {
            // 处理 basic 分组配置更新
        }
    }
}
```

## 推荐：配置服务封装模式

为了更好地管理插件配置，建议创建专门的配置服务类，将配置实体和获取逻辑封装在一起：

### 1. 定义配置实体

```java
/**
 * 基础配置
 */
public record BasicSetting(
    boolean enabled,
    String apiKey,
    int timeout,
    int maxRetries
) {
    public static final String GROUP = "basic";
    
    // 提供默认值
    public static BasicSetting defaultSetting() {
        return new BasicSetting(true, "", 30, 3);
    }
}

/**
 * 通知配置
 */
public record NotificationSetting(
    boolean enableNotification,
    String webhookUrl,
    List<String> recipients
) {
    public static final String GROUP = "notification";
    
    public static NotificationSetting defaultSetting() {
        return new NotificationSetting(false, "", List.of());
    }
}
```

### 2. 定义配置服务接口

```java
/**
 * 插件配置服务接口
 */
public interface PluginSettingService {
    
    /**
     * 获取基础配置
     */
    Mono<BasicSetting> getBasicSetting();
    
    /**
     * 获取通知配置
     */
    Mono<NotificationSetting> getNotificationSetting();
    
    /**
     * 检查插件是否启用
     */
    Mono<Boolean> isEnabled();
    
    /**
     * 获取 API Key
     */
    Mono<String> getApiKey();
}
```

### 3. 实现配置服务

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PluginSettingServiceImpl implements PluginSettingService {
    
    private final ReactiveSettingFetcher settingFetcher;
    
    @Override
    public Mono<BasicSetting> getBasicSetting() {
        return settingFetcher.fetch(BasicSetting.GROUP, BasicSetting.class)
            .defaultIfEmpty(BasicSetting.defaultSetting())
            .doOnError(e -> log.error("Failed to fetch basic setting", e))
            .onErrorReturn(BasicSetting.defaultSetting());
    }
    
    @Override
    public Mono<NotificationSetting> getNotificationSetting() {
        return settingFetcher.fetch(NotificationSetting.GROUP, NotificationSetting.class)
            .defaultIfEmpty(NotificationSetting.defaultSetting())
            .onErrorReturn(NotificationSetting.defaultSetting());
    }
    
    @Override
    public Mono<Boolean> isEnabled() {
        return getBasicSetting().map(BasicSetting::enabled);
    }
    
    @Override
    public Mono<String> getApiKey() {
        return getBasicSetting().map(BasicSetting::apiKey);
    }
}
```

### 4. 配置变更监听器

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PluginSettingChangeListener {
    
    private final PluginSettingService settingService;
    
    @EventListener
    public void onConfigUpdated(PluginConfigUpdatedEvent event) {
        var newValues = event.getNewSettingValues();
        var oldValues = event.getOldSettingValues();
        
        // 检查基础配置是否变更
        if (isGroupChanged(BasicSetting.GROUP, oldValues, newValues)) {
            handleBasicSettingChange();
        }
        
        // 检查通知配置是否变更
        if (isGroupChanged(NotificationSetting.GROUP, oldValues, newValues)) {
            handleNotificationSettingChange();
        }
    }
    
    private boolean isGroupChanged(String group, 
            Map<String, JsonNode> oldValues, 
            Map<String, JsonNode> newValues) {
        var oldValue = oldValues.get(group);
        var newValue = newValues.get(group);
        return !Objects.equals(oldValue, newValue);
    }
    
    private void handleBasicSettingChange() {
        settingService.getBasicSetting()
            .doOnNext(setting -> {
                log.info("Basic setting updated: enabled={}", setting.enabled());
                // 执行相关操作，如重新初始化客户端等
            })
            .subscribe();
    }
    
    private void handleNotificationSettingChange() {
        settingService.getNotificationSetting()
            .doOnNext(setting -> {
                log.info("Notification setting updated: enabled={}", 
                    setting.enableNotification());
            })
            .subscribe();
    }
}
```

### 5. 在业务代码中使用

```java
@Service
@RequiredArgsConstructor
public class MyBusinessService {
    
    private final PluginSettingService settingService;
    private final WebClient webClient;
    
    public Mono<Void> doSomething() {
        return settingService.isEnabled()
            .filter(enabled -> enabled)
            .flatMap(enabled -> settingService.getApiKey())
            .flatMap(apiKey -> {
                // 使用 apiKey 调用外部服务
                return webClient.get()
                    .uri("/api/data")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(Void.class);
            });
    }
    
    public Mono<Void> sendNotification(String message) {
        return settingService.getNotificationSetting()
            .filter(NotificationSetting::enableNotification)
            .flatMap(setting -> {
                // 发送通知
                return webClient.post()
                    .uri(setting.webhookUrl())
                    .bodyValue(Map.of("message", message))
                    .retrieve()
                    .bodyToMono(Void.class);
            });
    }
}
```

### 配置服务模式的优势

1. **类型安全** - 使用 record 或 class 定义配置实体，编译时检查
2. **默认值处理** - 统一处理配置缺失或解析失败的情况
3. **错误处理** - 集中处理配置获取异常
4. **易于测试** - 可以 mock 配置服务进行单元测试
5. **代码复用** - 避免在多处重复获取配置的代码
6. **变更响应** - 统一处理配置变更事件

## FormKit 内置组件

### 文本输入

```yaml
# 单行文本
- $formkit: text
  name: title
  label: 标题
  value: ""                    # 默认值
  placeholder: 请输入标题
  validation: required         # 验证规则
  help: 这是帮助文本

# 多行文本
- $formkit: textarea
  name: description
  label: 描述
  rows: 5
  
# 密码输入
- $formkit: password
  name: apiKey
  label: API Key
```

### 数字输入

```yaml
- $formkit: number
  name: maxRetries
  label: 最大重试次数
  value: 3
  min: 1
  max: 10
  step: 1
```

### 选择组件

```yaml
# 下拉选择
- $formkit: select
  name: mode
  label: 运行模式
  value: normal
  options:
    - label: 正常模式
      value: normal
    - label: 调试模式
      value: debug
    - label: 静默模式
      value: silent

# 单选按钮组
- $formkit: radio
  name: priority
  label: 优先级
  value: medium
  options:
    - label: 高
      value: high
    - label: 中
      value: medium
    - label: 低
      value: low

# 多选框组
- $formkit: checkbox
  name: features
  label: 启用功能
  options:
    - label: 功能A
      value: featureA
    - label: 功能B
      value: featureB
```

### 开关

```yaml
- $formkit: checkbox
  name: enabled
  label: 启用插件
  value: true
```

### 颜色选择

```yaml
- $formkit: color
  name: themeColor
  label: 主题色
  value: "#1890ff"
```

## Halo 自定义组件

### 附件选择 (attachment)

```yaml
- $formkit: attachment
  name: logo
  label: Logo
  accepts:
    - "image/*"
```

### 代码编辑器 (code)

```yaml
- $formkit: code
  name: customScript
  label: 自定义脚本
  language: javascript       # 支持: yaml, html, css, javascript, json
  height: 200px
```

### 内容选择

```yaml
# 文章选择
- $formkit: postSelect
  name: featuredPost
  label: 推荐文章

# 页面选择
- $formkit: singlePageSelect
  name: targetPage
  label: 目标页面

# 分类选择
- $formkit: categorySelect
  name: category
  label: 分类
  multiple: false

# 标签选择
- $formkit: tagSelect
  name: tags
  label: 标签
  multiple: true
```

### 数组 (array) ⭐ 推荐

```yaml
- $formkit: array
  name: webhooks
  label: Webhook 配置
  addLabel: 添加 Webhook
  emptyText: 暂无配置
  min: 0
  max: 10
  itemLabels:
    - type: text
      label: $value.name
    - type: text
      label: $value.url
  children:
    - $formkit: text
      name: name
      label: 名称
      validation: required
    - $formkit: text
      name: url
      label: URL
      validation: required|url
    - $formkit: select
      name: method
      label: 请求方法
      options:
        - label: POST
          value: POST
        - label: GET
          value: GET
```

### 密钥管理 (secret)

```yaml
- $formkit: secret
  name: credentials
  label: API 凭证
  requiredKeys:
    - key: clientId
      help: 请输入 Client ID
    - key: clientSecret
      help: 请输入 Client Secret
```

### 远程验证 (verificationForm)

```yaml
- $formkit: verificationForm
  name: connectionTest
  label: 连接测试
  action: /apis/my-plugin.halo.run/v1alpha1/test-connection
  children:
    - $formkit: text
      name: host
      label: 服务器地址
    - $formkit: number
      name: port
      label: 端口
```

## 验证规则

```yaml
- $formkit: text
  name: email
  label: 邮箱
  validation: required|email

- $formkit: text
  name: url
  label: 网址
  validation: required|url

- $formkit: text
  name: apiKey
  label: API Key
  validation: required|length:32,64
```

常用验证规则：
- `required` - 必填
- `email` - 邮箱格式
- `url` - URL 格式
- `length:min,max` - 长度范围
- `min:value` / `max:value` - 数值范围
- `matches:/regex/` - 正则匹配

## 条件显示

```yaml
- $formkit: checkbox
  name: enableAdvanced
  label: 启用高级设置
  value: false

- $formkit: text
  name: advancedOption
  label: 高级选项
  if: "$get(enableAdvanced).value"
```

## 完整示例

```yaml
apiVersion: v1alpha1
kind: Setting
metadata:
  name: my-plugin-setting
spec:
  forms:
    - group: basic
      label: 基本设置
      formSchema:
        - $formkit: checkbox
          name: enabled
          label: 启用插件
          value: true
        - $formkit: text
          name: apiKey
          label: API Key
          validation: required
          help: 从服务商获取的 API Key
          
    - group: advanced
      label: 高级设置
      formSchema:
        - $formkit: number
          name: timeout
          label: 超时时间(秒)
          value: 30
          min: 5
          max: 300
        - $formkit: number
          name: maxRetries
          label: 最大重试次数
          value: 3
          min: 0
          max: 10
        - $formkit: select
          name: logLevel
          label: 日志级别
          value: info
          options:
            - label: DEBUG
              value: debug
            - label: INFO
              value: info
            - label: WARN
              value: warn
            - label: ERROR
              value: error
              
    - group: notification
      label: 通知设置
      formSchema:
        - $formkit: checkbox
          name: enableNotification
          label: 启用通知
          value: false
        - $formkit: array
          name: recipients
          label: 通知接收人
          if: "$get(enableNotification).value"
          addLabel: 添加接收人
          itemLabels:
            - type: text
              label: $value.email
          children:
            - $formkit: text
              name: name
              label: 姓名
            - $formkit: text
              name: email
              label: 邮箱
              validation: required|email
```
