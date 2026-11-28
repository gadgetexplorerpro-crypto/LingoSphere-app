const isProductionBuild =
  typeof import.meta !== 'undefined' && Boolean((import.meta as { env?: { PROD?: boolean } }).env?.PROD);

export const USE_TEST_ADS = !isProductionBuild; // automatically uses real AdMob IDs in production builds

const REAL = {
  appId: 'ca-app-pub-8471293855339658~7525338937',
  banner: 'ca-app-pub-8471293855339658/8524799689',
  interstitial: 'ca-app-pub-8471293855339658/6979336613',
  rewardedInterstitial: 'ca-app-pub-8471293855339658/5666254944',
  rewarded: 'ca-app-pub-8471293855339658/1933847310',
  // native (2376700449) and app open (3080901315) are NOT supported by @capacitor-community/admob, so they are unused
};

const TEST = {
  appId: 'ca-app-pub-3940256099942544~3347511713',
  banner: 'ca-app-pub-3940256099942544/6300978111',
  interstitial: 'ca-app-pub-3940256099942544/1033173712',
  rewardedInterstitial: 'ca-app-pub-3940256099942544/5354046379',
  rewarded: 'ca-app-pub-3940256099942544/5224354917',
};

export const ADS = { ...(USE_TEST_ADS ? TEST : REAL), testing: USE_TEST_ADS };
export const PRODUCTION_ADMOB_IDS = REAL;
export const GOOGLE_TEST_ADMOB_IDS = TEST;
export const ADS_CONFIG = ADS;
