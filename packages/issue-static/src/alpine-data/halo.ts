import {axiosInstance, Policy, PolicyList, Group, GroupList, AttachmentList, Attachment} from "@halo-dev/api-client";
import {Moment, MomentMedia, MomentMediaTypeEnum, MomentSpecVisibleEnum, UploadResult} from "../types";
import {PositionInfo} from "../types/position-info";

interface HaloGeneralFun {
  showMainMenu: boolean;
  friendListModeKey:string;
  uploadGroup: string|null,
  uploadPolicy: string,
  uploadResults:Array<UploadResult>,
  randomPost():void;
  addQueryParams(params: { [key: string]: string | number }, refreshPage:boolean): void;
  hasTargetKey(key:string):boolean;
  hasTargetParam(key: string): boolean;
  removeAllQueryParams(): void;
  removeSpecialKey (key:string, refresh:boolean):void;
  getSpecialParam(key:string):string|number|boolean;
  setFriendPostShowMode(val:boolean):void;
  getFriendPostShowMode(key:string): boolean;
  statistics51la(monitorID:string):void;
  extractTextContent(content:string ):string;
  filterMovieGenre(genres:string):boolean;
  listAttachments(page:number, size:number, group:string, policy:string, keyword:string):Promise<AttachmentList>;
  listAttachmentGroups():Promise<Array<Group>>;
  listAttachmentPolicies():Promise<Array<Policy>>;
  publishMoment(username:string, raw:string,tagList:Array<string>, medium:Array<MomentMedia>,visible:string, position:string):Promise<boolean>;
  attachmentAcceptCheck(attachment:Attachment):boolean;
  selectedAttachment(attachment:Attachment):MomentMedia;
  handlerAttachmentUpload(event:Event):void;
  queryCurrentPosition():Promise<string>;
  typeOutputText (text:string, speed:number):void,
  handlerPostSideBarShow():boolean;
}

export default ():HaloGeneralFun => ({
  showMainMenu: false,
  friendListModeKey: 'friendListMode',
  uploadGroup: '', 
  uploadPolicy: '',
  uploadResults: [],
  randomPost: function (){
    axiosInstance.get("/apis/api.microimmersion.webjing.cn/v1alpha1/random/post")
      .then(result=>{
        if(result.status == 200) {
          const post = result.data;
          const displayDom = document.querySelector("#randomPostDisplay");
          if(displayDom) {
            displayDom.setAttribute("href", post.status.permalink);
            displayDom.innerHTML = post.spec.title;
          }
        }
      });
  },
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
  },
  setFriendPostShowMode: function (val:boolean){
    const stringData = JSON.stringify(val);
    localStorage.setItem(this.friendListModeKey, stringData);
  },
  getFriendPostShowMode: function ():boolean{
    const stringData = localStorage.getItem(this.friendListModeKey);
    if(stringData){
      const data = JSON.parse(stringData);
      return Boolean(data);
    }
    return false;
  },
  statistics51la: async function (monitorID:string){
    if(!monitorID){
      return {titleList: [], statistic: []};
    }
    const result = await fetch(`https://v6-widget.51.la/v6/${monitorID}/quote.js`);
    const data = await result.text();
    const titleList:string[] = ["最近活跃", "今日人数", "今日访问", "昨日人数", "昨日访问", "本月访问", "总访问量"];
    let statistic: string[] = data.match(/(<\/span><span>).*?(\/span><\/p>)/g) as string [];
    if(statistic != null){
      statistic = statistic.map(el => {
        const val = el.replace(/(<\/span><span>)/g, "");
        const datasStr = val.replace(/(<\/span><\/p>)/g, "");
        return datasStr;
      });
    }
    return {titleList, statistic};
  },
  extractTextContent: function (content:string):string {
    // 解析 html字符串
    const parser = new DOMParser();
    const doc = parser.parseFromString(content, 'text/html');
    const contentDiv = doc.querySelector('body') as HTMLElement;
    // 提取纯文本内容
    const textContent = contentDiv.textContent || contentDiv.innerText;
    return textContent;
  },
  // 过滤豆瓣电影类型
  filterMovieGenre: function (genres:string):boolean{
    if(!genres) {
      return  true;
    }
    // 处理字符串，分割字符串
    const genreArr = genres.replace("[",'').replace(']', '').split(',');
    const curGenre = this.getSpecialParam("genreName") as string;
    if(!curGenre){
      return true;
    }else{
      // 匹配类型
      const decodeGenre = decodeURIComponent(curGenre);
      const patchRes = genreArr.filter(item=> item.trim() === decodeGenre);
      return patchRes.length ? true : false;
    }
  },
  listAttachments: async function (page:number, size:number, group:string, policy:string, keyword:string):Promise<AttachmentList>{
    let queryParams = `?page=${page}&size=${size}`;
    if(policy){
      queryParams += `&fieldSelector=spec.policyName=${policy}`;
    }
    if(group){
      if(group == "ungrouped") {
        queryParams += '&ungrouped=true';
      }else{
        queryParams += `&fieldSelector=spec.groupName=${group}`;
      }
    }
    if(keyword){
      queryParams += `&keyword=${keyword}`;
    }
    const response = await  axiosInstance.get("/apis/api.console.halo.run/v1alpha1/attachments" + queryParams);
    let attachmentList = {} as AttachmentList;
    if(response.status == 200){
      attachmentList = response.data as AttachmentList;
    }
    return attachmentList;
  },
  listAttachmentGroups: async function ():Promise<Array<Group>>{
    const response = await  axiosInstance.get("/apis/storage.halo.run/v1alpha1/groups",
      {
        params: {
          labelSelector: "!halo.run/hidden",
          sort: "metadata.creationTimestamp,asc"
        }
      }
    );
    let groupItems:Group[] = [];
    if(response.status == 200){
      const groupList = response.data  as GroupList
      groupItems = groupList.items;
    }
    return groupItems;
  },
  listAttachmentPolicies: async function ():Promise<Array<Policy>>{
    const response = await axiosInstance.get("/apis/storage.halo.run/v1alpha1/policies");
    let policyItems:Array<Policy> = [];
    if(response.status == 200){
      const policyList = response.data as PolicyList;
      policyItems = policyList.items
    }
    return policyItems;
  },
  publishMoment: async function (username:string, raw:string, tagList:Array<string>, medium:Array<MomentMedia>, visible:string, position:string):Promise<boolean>{
    let parseHtml =`<p>${raw}</p>`;
    let tagListHtml = '';
    if(tagList.length){
      tagList.forEach(tagItem => {
        tagListHtml += `<a href="?tag=${tagItem}" class="tag">${tagItem}</a>`;
      })
      parseHtml += `<p class="tag-list">${tagListHtml}</p>`;
    }
    const initMoment:Moment = {
      spec: {
        content: {
          raw: parseHtml,
          html: parseHtml,
          medium: medium,
        },
        releaseTime: new Date().toISOString(),
        owner: username,
        visible: visible == MomentSpecVisibleEnum.Public ? 'PUBLIC' : 'PRIVATE',
        tags: tagList,
        approved: true,
      },
      metadata: {
        generateName: "moment-",
        name: "",
      },
      kind: "Moment",
      apiVersion: "moment.halo.run/v1alpha1",
    };
    if(position){
      // @ts-ignore
      initMoment.metadata.annotations = { 'momentPosition' : position };
    }
    const createResult = await axiosInstance.post("/apis/console.api.moment.halo.run/v1alpha1/moments", initMoment);
    if(createResult.status == 200){
      return true
    }
    return false;
  },
  attachmentAcceptCheck:function(attachment:Attachment):boolean{
    const attachmentType = attachment.spec.mediaType as string;
    const patchType = [];
    if(attachmentType.startsWith('image/')){
      patchType.push("image/*");
    }
    if(attachmentType.startsWith('video/')){
      patchType.push("video/*");
    }
    if(!patchType.length){
      return false;
    }
    return true;
  },
  selectedAttachment: function (attachment:Attachment):MomentMedia{
    const initMomentMedia:MomentMedia = {
      originType: attachment.spec.mediaType,
      type: 'PHOTO',
      url: attachment.status?.permalink
    }
    const mediaType = attachment.spec.mediaType as string;
    // 设置媒体类型
    if(mediaType.startsWith('image/')){
      initMomentMedia.type = MomentMediaTypeEnum.Photo;
    }
    if(mediaType.startsWith('video/')){
      initMomentMedia.type = MomentMediaTypeEnum.Video;
    }
    return initMomentMedia;
  },
  handlerAttachmentUpload: async function (event:Event){
    const uploadUrl = "/apis/api.console.halo.run/v1alpha1/attachments/upload";
    const uploadEle = document.querySelector("#uploadAttachment");
    event.preventDefault();
    // @ts-ignore
    const fileList = uploadEle.files;
    if(fileList.length){
      const resultList = await Promise.all(
        Array.from(fileList).map(async fileItem =>{
          const initUploadResult:UploadResult = {
            status: false,
            message: '',
            displayName: null,
            url: null,
            type: null
          };
          const data = new FormData();
          // @ts-ignore
          data.append('file', fileItem);
          data.append('groupName', this.uploadGroup || '');
          this.uploadPolicy && data.append('policyName', this.uploadPolicy);
          try{
            const result = await axiosInstance.post(uploadUrl, data);
            if(result.status == 200){
              initUploadResult.status = true;
              // @ts-ignore
              initUploadResult.message = `文件 ${fileItem.name} 上传成功！`;
              const attachmentObj = result.data;
              initUploadResult.url = attachmentObj.metadata.annotations["storage.halo.run/uri"] || attachmentObj.metadata.annotations["storage.halo.run/external-link"] || attachmentObj.metadata.annotations["lskypro.plugin.halo.chenhe.me/image-link"];
              initUploadResult.displayName = attachmentObj.spec.displayName;
              if(attachmentObj.spec.mediaType?.startsWith("image/")){
                initUploadResult.type = 'image';
              }else if(attachmentObj.spec.mediaType?.startsWith("video/")){
                initUploadResult.type = 'video';
              }else if(attachmentObj.spec.mediaType?.startsWith("audio/")){
                initUploadResult.type = 'audio';
              }
            }else {
              initUploadResult.status = false;
              // @ts-ignore
              initUploadResult.message = `文件 ${fileItem.name} 上传失败：${result.data}`;
              // @ts-ignore
              initUploadResult.displayName = fileItem.name;
            }
          }catch (error) {
            initUploadResult.status = false;
            // @ts-ignore
            initUploadResult.message = `文件 ${fileItem.name} 上传失败：${error.message}`;
            // @ts-ignore
            initUploadResult.displayName = fileItem.name;
          }
          return initUploadResult;
        })
      );
      this.uploadResults.push(...resultList);
    }
  },
  queryCurrentPosition: async function ():Promise<string>{
    const result = await axiosInstance.get("/apis/api.microimmersion.webjing.cn/v1alpha1/position/moment")
    if(result.status == 200) {
      const positionInfo = result.data as PositionInfo;
      if(positionInfo.status == 0){
        return positionInfo.province + " " + positionInfo.city;
      }else{
        return "";
      }
    }
    return "";
  },
  typeOutputText: function (text:string, speed:number):void {
    const aiSummary = document.getElementById("aiSummaryWrapper");
    let index = 0;
    function type() {
      if (index < text.length) {
        if (aiSummary) {
          aiSummary.innerText += text.charAt(index);
        }
        index++;
        setTimeout(type, speed);
      }
    }
    type();
  },
  handlerPostSideBarShow: ():boolean =>{
    if(window.innerWidth >= 1024){
      return true;
    }else{
      return false;
    }
  },

});


