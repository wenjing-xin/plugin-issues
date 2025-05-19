export default () => ({
  isVisible: false,
  type: '',
  text: '',
  timer: null,

  showMessage(type:string, text:string, duration:number) {
    this.type = type;
    this.text = text;
    this.isVisible = true;
    // 清除之前的定时器
    if (this.timer) {
      clearTimeout(this.timer);
    }

    // 设置新的定时器
    // @ts-ignore
    this.timer = setTimeout(() => {
      this.hideMessage();
    }, duration);
  },

  hideMessage() {
    this.isVisible = false;
  }
})