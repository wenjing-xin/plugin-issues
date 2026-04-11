import { createAvatar } from '@dicebear/core';
import { initials, adventurerNeutral, funEmoji, glass, notionistsNeutral, lorelei } from '@dicebear/collection';

export default (avatarType:string, size:number) => ({
    createAvatarByUserName(username:string){
        let avatar = null;
        let config = {
            seed: username,
            size: size
        }
        if(avatarType === 'initials'){
            avatar = createAvatar(initials, config);
        }else if(avatarType === 'adventurerNeutral'){
            avatar = createAvatar(adventurerNeutral, config);
        }else if(avatarType === 'FunEmoji'){
            avatar = createAvatar(funEmoji, config);
        }else if(avatarType === 'Glass'){
            avatar = createAvatar(glass, config);
        }else if(avatarType === 'NotionistsNeutral'){
            avatar = createAvatar(notionistsNeutral, config);
        }else if(avatarType === 'lorelei'){
            avatar = createAvatar(lorelei, config);
        }
        else{
            avatar = createAvatar(initials, config);
        }
        const dataUri = avatar.toDataUri();
        return dataUri;
    }
})