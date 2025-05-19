/**
 * Media item of moment
 * @export
 * @interface MomentMedia
 */
export interface MomentMedia {
  /**
   * Origin type of media.
   * @type {string}
   * @memberof MomentMedia
   */
  'originType'?: string;
  /**
   * Type of media
   * @type {string}
   * @memberof MomentMedia
   */
  'type'?: MomentMediaTypeEnum;
  /**
   * External URL of media
   * @type {string}
   * @memberof MomentMedia
   */
  'url'?: string;
}

export const MomentMediaTypeEnum = {
  Photo: 'PHOTO',
  Video: 'VIDEO',
  Post: 'POST',
  Audio: 'AUDIO'
} as const;

export type MomentMediaTypeEnum = typeof MomentMediaTypeEnum[keyof typeof MomentMediaTypeEnum];