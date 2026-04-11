export default () => ({
  isVisible: false,
  type: '',
  text: '',
  timer: null,

  showMessage(type: string, text: string, duration: number) {
    this.type = type;
    this.text = text;
    this.isVisible = true;
    // 清除之前的定时器
    if (this.timer) {
      clearTimeout(this.timer);
    }

    // ===== 新增：DOM 展示逻辑 =====
    let messageDiv = document.getElementById('global-message-tip') as HTMLDivElement;
    if (!messageDiv) {
      messageDiv = document.createElement('div');
      messageDiv.id = 'global-message-tip';
      document.body.appendChild(messageDiv);
    }
    messageDiv.textContent = text;
    messageDiv.style.position = 'fixed';
    messageDiv.style.top = '15px';
    messageDiv.style.left = '50%';
    messageDiv.style.transform = 'translateX(-50%)';
    messageDiv.style.padding = '8px 20px';
    messageDiv.style.borderRadius = '6px';
    messageDiv.style.fontSize = '14px';
    messageDiv.style.color = '#fff';
    messageDiv.style.boxShadow = '0 2px 12px rgba(0,0,0,0.08)';
    messageDiv.style.zIndex = '999';
    messageDiv.style.opacity = '1';
    messageDiv.style.transition = 'opacity 0.3s, top 0.3s';
    switch (type) {
      case 'success':
        messageDiv.style.background = '#67C23A';
        break;
      case 'error':
        messageDiv.style.background = '#F56C6C';
        break;
      case 'warning':
        messageDiv.style.background = '#E6A23C';
        break;
      default:
        messageDiv.style.background = '#909399';
    }
    // ===== 结束 =====

    // 设置新的定时器
    // @ts-ignore
    this.timer = setTimeout(() => {
      this.hideMessage();
      // ===== 新增：隐藏动画并移除DOM =====
      messageDiv.style.opacity = '0';
      setTimeout(() => {
        if (messageDiv && messageDiv.parentNode) {
          messageDiv.parentNode.removeChild(messageDiv);
        }
      }, 300);
      // ===== 结束 =====
    }, duration);
  },

  hideMessage() {
    this.isVisible = false;
  }
})