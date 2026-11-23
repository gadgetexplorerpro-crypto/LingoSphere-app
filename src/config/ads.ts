/**
 * LingoSphere Google AdMob Configuration
 *
 * Set USE_TEST_ADS = false before generating a production release APK/AAB.
 * When USE_TEST_ADS is true (default for development), official Google Test Ad Unit IDs are used.
 * When USE_TEST_ADS is false, LingoSphere production AdMob IDs are used.
 */

export const USE_TEST_ADS: boolean = true;

export interface AdMobUnitConfig {
  readonly appId: string;
  readonly banner: string;
  readonly interstitial: string;
  readonly rewardedInterstitial: string;
  readonly rewarded: string;
  readonly nativeAdvanced: string;
  readonly appOpen: string;
}

export const PRODUCTION_ADMOB_IDS: AdMobUnitConfig = {
  appId: 'ca-app-pub-8471293855339658~7525338937',
  banner: 'ca-app-pub-8471293855339658/8524799689',
  interstitial: 'ca-app-pub-8471293855339658/6979336613',
  rewardedInterstitial: 'ca-app-pub-8471293855339658/5666254944',
  rewarded: 'ca-app-pub-8471293855339658/1933847310',
  nativeAdvanced: 'ca-app-pub-8471293855339658/2376700449',
  appOpen: 'ca-app-pub-8471293855339658/3080901315',
};

export const GOOGLE_TEST_ADMOB_IDS: AdMobUnitConfig = {
  appId: 'ca-app-pub-3940256099942544~3347511713',
  banner: 'ca-app-pub-3940256099942544/6300978111',
  interstitial: 'ca-app-pub-3940256099942544/1033173712',
  rewardedInterstitial: 'ca-app-pub-3940256099942544/5354046379',
  rewarded: 'ca-app-pub-3940256099942544/5224354917',
  nativeAdvanced: 'ca-app-pub-3940256099942544/2247696110',
  appOpen: 'ca-app-pub-3940256099942544/9257395921',
};

export const ADS_CONFIG: AdMobUnitConfig = USE_TEST_ADS
  ? GOOGLE_TEST_ADMOB_IDS
  : PRODUCTION_ADMOB_IDS;
