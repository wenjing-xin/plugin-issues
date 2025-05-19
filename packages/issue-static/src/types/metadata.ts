/**
 *
 * @export
 * @interface Metadata
 */
export interface Metadata {
  /**
   *
   * @type {{ [key: string]: string; }}
   * @memberof Metadata
   */
  'annotations'?: { [key: string]: string; };
  /**
   *
   * @type {string}
   * @memberof Metadata
   */
  'creationTimestamp'?: string | null;
  /**
   *
   * @type {string}
   * @memberof Metadata
   */
  'deletionTimestamp'?: string | null;
  /**
   *
   * @type {Array<string>}
   * @memberof Metadata
   */
  'finalizers'?: Array<string> | null;
  /**
   * The name field will be generated automatically according to the given generateName field
   * @type {string}
   * @memberof Metadata
   */
  'generateName'?: string;
  /**
   *
   * @type {{ [key: string]: string; }}
   * @memberof Metadata
   */
  'labels'?: { [key: string]: string; };
  /**
   * Metadata name
   * @type {string}
   * @memberof Metadata
   */
  'name': string;
  /**
   *
   * @type {number}
   * @memberof Metadata
   */
  'version'?: number | null;
}