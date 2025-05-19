// May contain unused imports in some cases
// @ts-ignore
import type { MomentMedia } from './moment-media';

/**
 *
 * @export
 * @interface MomentContent
 */
export interface MomentContent {
  /**
   * Rendered result with HTML format
   * @type {string}
   * @memberof MomentContent
   */
  'html'?: string;
  /**
   * Medium of moment
   * @type {Array<MomentMedia>}
   * @memberof MomentContent
   */
  'medium'?: Array<MomentMedia>;
  /**
   * Raw of content
   * @type {string}
   * @memberof MomentContent
   */
  'raw'?: string;
}