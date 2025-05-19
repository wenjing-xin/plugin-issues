import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import timezone from "dayjs/plugin/timezone";
import utc from "dayjs/plugin/utc";
import relativeTime from "dayjs/plugin/relativeTime";

dayjs.extend(timezone);
dayjs.extend(utc);
dayjs.extend(relativeTime);

dayjs.locale("zh-cn");
export default () => ({
   formatDatetime: (date: string | Date | undefined | null, tz?: string): string =>{
    if (!date) {
      return "";
    }
    return dayjs(date).tz(tz).format("YYYY-MM-DD HH:mm");
  },
  relativeTimeTo: (date: string | Date | undefined | null) => {
    if (!date) {
      return;
    }
    return dayjs().to(dayjs(date));
  }

});
