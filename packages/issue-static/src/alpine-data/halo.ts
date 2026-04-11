
interface HaloGeneralFun {
  showMainMenu: boolean;
  friendListModeKey:string;
  uploadGroup: string|null,
  uploadPolicy: string,
  addQueryParams(params: { [key: string]: string | number }, refreshPage:boolean): void;
  hasTargetKey(key:string):boolean;
  hasTargetParam(key: string): boolean;
  removeAllQueryParams(): void;
  removeSpecialKey (key:string, refresh:boolean):void;
  getSpecialParam(key:string):string|number|boolean;
}

export default ():HaloGeneralFun => ({
  showMainMenu: false,
  friendListModeKey: 'friendListMode',
  uploadGroup: '', 
  uploadPolicy: '',

  addQueryParams: function(params: { [key: string]: string | number }, refreshPage:boolean) {
    const url = new URL(window.location.href);
    const searchParams = new URLSearchParams(url.search);
    for (const key in params) {
      if (Object.prototype.hasOwnProperty.call(params, key)) {
        searchParams.set(key, params[key].toString());
      }
    }
    url.search = searchParams.toString();
    window.history.pushState({}, '', url.toString());
    if(refreshPage){
      window.location.href = url.toString(); // refresh pages
    }
  },
  hasTargetKey:function (key:string):boolean{
    if(!window.location.search || !key){
      return false;
    }
    const splitRes = window.location.search.substring(1).split("&");
    // @ts-ignore
    const patchRes = splitRes.filter((item:string) => {
      const keyValArr = item.split("=");
      if(keyValArr[0] == key){
        return item;
      }
    });
    return patchRes.length == 0 ? false :true;
  },
  // url中是否含有指定的参数 返回 true 和 false
  hasTargetParam: function (key:string):boolean{
    if(!window.location.search || !key){
      return false;
    }
    const splitRes = window.location.search.substring(1).split("&");
    // @ts-ignore
    const patchRes = splitRes.filter((item:string) => {
      const keyValArr = item.split("=");
      if(keyValArr[1] == key){
        return item;
      }
    });
    return patchRes.length == 0 ? false :true;
  },
  removeAllQueryParams: function () {
    const url = new URL(window.location.href);
    url.search = '';
    window.history.pushState({}, '', url.toString());
    window.location.href = url.toString(); // refresh pages
  },
  removeSpecialKey: function (key:string, refresh:boolean){
    const url = new URL(window.location.href);
    url.searchParams.delete(key);
    window.history.pushState({}, '', url.toString());
    if(refresh){
      window.location.href = url.toString(); // 刷新页面
    }
  },
  // 获取指定的路径参数
  getSpecialParam: function (key:string):string|number|boolean{
    if(!key || !window.location.search){
      return "";
    }
    const params = window.location.search.substring(1).split("&");
    const patchRes = params.filter(item => item.split("=")[0] === key);
    if(patchRes && patchRes.length){
      return patchRes[0].split("=")[1];
    }
    return "";
  }



});


