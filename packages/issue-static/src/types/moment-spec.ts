// May contain unused imports in some cases
// @ts-ignore
import type { MomentContent } from './moment-content';

/**
 *
 * @export
 * @interface MomentSpec
 */
export interface MomentSpec {
  /**
   *
   * @type {boolean}
   * @memberof MomentSpec
   */
  'approved'?: boolean;
  /**
   *
   * @type {string}
   * @memberof MomentSpec
   */
  'approvedTime'?: string;
  /**
   *
   * @type {MomentContent}
   * @memberof MomentSpec
   */
  'content': MomentContent;
  /**
   * Owner of the moment
   * @type {string}
   * @memberof MomentSpec
   */
  'owner': string;
  /**
   * Release timestamp. This field can be customized by owner
   * @type {string}
   * @memberof MomentSpec
   */
  'releaseTime'?: string;
  /**
   * Tags of the moment
   * @type {Array<string>}
   * @memberof MomentSpec
   */
  'tags'?: Array<string>;
  /**
   * Visible indicates when to show publicly. Default is public
   * @type {string}
   * @memberof MomentSpec
   */
  'visible'?: MomentSpecVisibleEnum;
}

export const MomentSpecVisibleEnum = {
  Public: 'PUBLIC',
  Private: 'PRIVATE'
} as const;

export type MomentSpecVisibleEnum = typeof MomentSpecVisibleEnum[keyof typeof MomentSpecVisibleEnum];