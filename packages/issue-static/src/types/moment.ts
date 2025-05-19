// May contain unused imports in some cases
// @ts-ignore
import type { Metadata } from './metadata';
// May contain unused imports in some cases
// @ts-ignore
import type { MomentSpec } from './moment-spec';
// May contain unused imports in some cases
// @ts-ignore
import type { MomentStatus } from './moment-status';

/**
 *
 * @export
 * @interface Moment
 */
export interface Moment {
  /**
   *
   * @type {string}
   * @memberof Moment
   */
  'apiVersion': string;
  /**
   *
   * @type {string}
   * @memberof Moment
   */
  'kind': string;
  /**
   *
   * @type {Metadata}
   * @memberof Moment
   */
  'metadata': Metadata;
  /**
   *
   * @type {MomentSpec}
   * @memberof Moment
   */
  'spec': MomentSpec;
  /**
   *
   * @type {MomentStatus}
   * @memberof Moment
   */
  'status'?: MomentStatus;
}