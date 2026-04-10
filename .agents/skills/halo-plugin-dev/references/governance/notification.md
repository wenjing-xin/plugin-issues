# Halo 插件通知系统开发指南

## 概述

Halo 通知系统支持插件扩展自定义通知方式（如邮件、短信、Webhook 等）和自定义通知事件类型。

## 核心概念

### 通知数据模型

- **ReasonType** - 事件类别定义，声明事件包含的数据属性
- **Reason** - 事件实例，触发通知时创建
- **Subscription** - 订阅关系，用户订阅感兴趣的事件
- **NotificationTemplate** - 通知模板，定义通知内容格式
- **NotifierDescriptor** - 通知器声明，描述通知方式
- **Notification** - 站内通知记录

## 实现自定义通知器

### 1. 实现 ReactiveNotifier 接口

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookNotifier implements ReactiveNotifier {

    private final WebClient webClient;

    @Override
    public Mono<Void> notify(NotificationContext context) {
        // 获取发送方配置（管理员配置）
        JsonNode senderConfig = context.getSenderConfig();
        String webhookUrl = senderConfig.get("webhookUrl").asText();
        
        // 获取接收方配置（用户配置）
        JsonNode receiverConfig = context.getReceiverConfig();
        
        // 获取消息内容
        NotificationContext.Message message = context.getMessage();
        NotificationContext.MessagePayload payload = message.getPayload();
        
        // 构建请求体
        Map<String, Object> body = Map.of(
            "title", payload.getTitle(),
            "content", payload.getRawBody(),
            "recipient", message.getRecipient(),
            "timestamp", message.getTimestamp().toString()
        );
        
        return webClient.post()
            .uri(webhookUrl)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnError(e -> log.error("发送 Webhook 通知失败", e))
            .onErrorResume(e -> Mono.empty());
    }
}
```

### 2. NotificationContext 结构

```java
@Data
public class NotificationContext {
    private Message message;           // 消息内容
    private ObjectNode receiverConfig; // 接收方配置（用户设置）
    private ObjectNode senderConfig;   // 发送方配置（管理员设置）

    @Data
    public static class Message {
        private MessagePayload payload;  // 消息载荷
        private Subject subject;         // 消息主体
        private String recipient;        // 接收者用户名
        private Instant timestamp;       // 时间戳
    }

    @Data
    public static class Subject {
        private String apiVersion;       // 如 content.halo.run/v1alpha1
        private String kind;             // 如 Post
        private String name;             // 资源名称
        private String title;            // 标题
        private String url;              // 访问链接
    }

    @Data
    public static class MessagePayload {
        private String title;            // 通知标题
        private String rawBody;          // 纯文本内容
        private String htmlBody;         // HTML 内容
        private ReasonAttributes attributes; // 事件属性
    }
}
```

### 3. 声明通知器扩展

`resources/extensions/notifier.yaml`:

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ExtensionDefinition
metadata:
  name: webhook-notifier-definition
spec:
  className: com.example.plugin.WebhookNotifier
  extensionPointName: reactive-notifier
  displayName: "Webhook 通知器"
  description: "通过 Webhook 发送通知"
```

### 4. 声明通知器描述

`resources/extensions/notifier-descriptor.yaml`:

```yaml
apiVersion: notification.halo.run/v1alpha1
kind: NotifierDescriptor
metadata:
  name: webhook-notifier
spec:
  displayName: 'Webhook 通知'
  description: '通过 Webhook 发送通知到第三方服务'
  notifierExtName: 'webhook-notifier-definition'
  senderSettingRef:
    name: 'webhook-notifier-sender-setting'
    group: 'sender'
  receiverSettingRef:
    name: 'webhook-notifier-receiver-setting'
    group: 'receiver'
```

### 5. 定义发送方设置（管理员配置）

`resources/extensions/sender-setting.yaml`:

```yaml
apiVersion: v1alpha1
kind: Setting
metadata:
  name: webhook-notifier-sender-setting
spec:
  forms:
    - group: sender
      label: Webhook 配置
      formSchema:
        - $formkit: checkbox
          name: enable
          label: 启用 Webhook 通知
          value: false
        - $formkit: text
          name: webhookUrl
          label: Webhook URL
          validation: required|url
          if: "$get(enable).value"
        - $formkit: secret
          name: credentials
          label: 认证信息
          if: "$get(enable).value"
          requiredKeys:
            - key: token
              help: 认证 Token（可选）
```

### 6. 定义接收方设置（用户配置）

`resources/extensions/receiver-setting.yaml`:

```yaml
apiVersion: v1alpha1
kind: Setting
metadata:
  name: webhook-notifier-receiver-setting
spec:
  forms:
    - group: receiver
      label: 接收设置
      formSchema:
        - $formkit: checkbox
          name: enabled
          label: 接收 Webhook 通知
          value: true
        - $formkit: text
          name: customEndpoint
          label: 自定义接收地址（可选）
          help: 留空则使用默认地址
```

## 定义自定义事件类型

### 1. 定义 ReasonType

`resources/extensions/reason-type.yaml`:

```yaml
apiVersion: notification.halo.run/v1alpha1
kind: ReasonType
metadata:
  name: new-order-received
spec:
  displayName: "收到新订单"
  description: "当用户下单时触发通知"
  properties:
    - name: orderNo
      type: string
      description: "订单编号"
    - name: customerName
      type: string
      description: "客户名称"
    - name: amount
      type: string
      description: "订单金额"
    - name: productName
      type: string
      description: "商品名称"
```

### 2. 定义通知模板

`resources/extensions/notification-template.yaml`:

```yaml
apiVersion: notification.halo.run/v1alpha1
kind: NotificationTemplate
metadata:
  name: new-order-template-zh
spec:
  reasonSelector:
    reasonType: new-order-received
    language: zh_CN
  template:
    title: "收到新订单 #[(${orderNo})]"
    rawBody: |
      您收到一个新订单：
      
      订单编号：[(${orderNo})]
      客户：[(${customerName})]
      商品：[(${productName})]
      金额：[(${amount})]
      
      请及时处理。
    htmlBody: |
      <div style="font-family: sans-serif;">
        <h2>您收到一个新订单</h2>
        <table style="border-collapse: collapse;">
          <tr>
            <td style="padding: 8px; border: 1px solid #ddd;">订单编号</td>
            <td style="padding: 8px; border: 1px solid #ddd;" th:text="${orderNo}"></td>
          </tr>
          <tr>
            <td style="padding: 8px; border: 1px solid #ddd;">客户</td>
            <td style="padding: 8px; border: 1px solid #ddd;" th:text="${customerName}"></td>
          </tr>
          <tr>
            <td style="padding: 8px; border: 1px solid #ddd;">商品</td>
            <td style="padding: 8px; border: 1px solid #ddd;" th:text="${productName}"></td>
          </tr>
          <tr>
            <td style="padding: 8px; border: 1px solid #ddd;">金额</td>
            <td style="padding: 8px; border: 1px solid #ddd;" th:text="${amount}"></td>
          </tr>
        </table>
      </div>
```

### 3. 触发通知事件

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ReactiveExtensionClient client;

    public Mono<Order> createOrder(Order order) {
        return client.create(order)
            .flatMap(savedOrder -> {
                // 创建通知事件
                return createNotificationReason(savedOrder)
                    .thenReturn(savedOrder);
            });
    }

    private Mono<Void> createNotificationReason(Order order) {
        var reason = new Reason();
        reason.setMetadata(new Metadata());
        reason.getMetadata().setGenerateName("order-notification-");
        
        var spec = new Reason.Spec();
        spec.setReasonType("new-order-received");
        spec.setAuthor(order.getSpec().getSellerId());
        
        // 设置主体信息
        var subject = new Reason.Subject();
        subject.setApiVersion("shop.example.com/v1alpha1");
        subject.setKind("Order");
        subject.setName(order.getMetadata().getName());
        subject.setTitle("订单 #" + order.getSpec().getOrderNo());
        subject.setUrl("/console/orders/" + order.getMetadata().getName());
        spec.setSubject(subject);
        
        // 设置事件属性
        var attributes = new HashMap<String, Object>();
        attributes.put("orderNo", order.getSpec().getOrderNo());
        attributes.put("customerName", order.getSpec().getCustomerName());
        attributes.put("amount", order.getSpec().getAmount().toString());
        attributes.put("productName", order.getSpec().getProductName());
        spec.setAttributes(attributes);
        
        reason.setSpec(spec);
        
        return client.create(reason).then();
    }
}
```

## 模板语法

通知模板使用 Thymeleaf 语法：

### 纯文本模板（rawBody）

使用 `textual` 模式：

```
订单编号：[(${orderNo})]
客户：[(${customerName})]
```

### HTML 模板（htmlBody）

使用标准 Thymeleaf 语法：

```html
<p th:text="${orderNo}"></p>
<a th:href="${site.url}">访问站点</a>
```

### 内置变量

所有模板都可以使用以下变量：

| 变量 | 说明 |
|------|------|
| `site.title` | 站点标题 |
| `site.subtitle` | 站点副标题 |
| `site.logo` | 站点 Logo |
| `site.url` | 站点 URL |
| `subscriber.id` | 订阅者 ID |
| `subscriber.displayName` | 订阅者显示名称 |
| `unsubscribeUrl` | 退订链接 |

## 用户订阅管理

### 创建订阅

```java
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final ReactiveExtensionClient client;

    public Mono<Subscription> subscribeToOrders(String username) {
        var subscription = new Subscription();
        subscription.setMetadata(new Metadata());
        subscription.getMetadata().setGenerateName("order-sub-");
        
        var spec = new Subscription.Spec();
        
        // 设置订阅者
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(username);
        spec.setSubscriber(subscriber);
        
        // 设置订阅的事件类型
        var reason = new Subscription.ReasonSubject();
        reason.setReasonType("new-order-received");
        spec.setReason(reason);
        
        // 生成退订 Token
        spec.setUnsubscribeToken(UUID.randomUUID().toString());
        
        subscription.setSpec(spec);
        
        return client.create(subscription);
    }
}
```

### 退订 API

退订链接格式：
```
/apis/api.notification.halo.run/v1alpha1/subscriptions/{name}/unsubscribe?token={unsubscribeToken}
```

## 邮件通知示例

Halo 内置了邮件通知器，插件可以直接使用：

```java
@Component
@RequiredArgsConstructor
public class MyEmailService {

    private final ReactiveExtensionClient client;

    public Mono<Void> sendEmailNotification(String recipient, String subject, String content) {
        // 创建 Reason 触发邮件通知
        var reason = new Reason();
        // ... 配置 reason
        
        return client.create(reason).then();
    }
}
```

用户需要在个人中心配置邮箱地址，并在通知偏好中启用邮件通知。

## 最佳实践

1. **模板多语言** - 为每种语言创建对应的 NotificationTemplate
2. **错误处理** - 通知发送失败时记录日志，不影响主流程
3. **配置验证** - 在 Setting 中添加验证规则
4. **退订支持** - 在通知内容中包含退订链接
5. **幂等性** - 避免重复发送相同通知
