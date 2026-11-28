import { Capacitor } from '@capacitor/core';
import {
  AdMob,
  BannerAdSize,
  BannerAdPosition,
  BannerAdPluginEvents,
  RewardAdPluginEvents,
  RewardInterstitialAdPluginEvents,
  InterstitialAdPluginEvents,
  AdmobConsentStatus,
  AdMobError,
} from '@capacitor-community/admob';
import { ADS } from '../config/ads';

const native = Capacitor.isNativePlatform();
let ready = false;
let lastInterstitial = 0;
let interstitialsThisSession = 0;
let currentUserIsVerifiedFree = false;

/**
 * Central rule: only Free, verified users on the native Android app see ads.
 * In web preview (!native) or for Pro / unverified users, returns false so NO ad ever renders.
 */
export function setAdEligibility(isLoggedInAndVerifiedFree: boolean): void {
  currentUserIsVerifiedFree = isLoggedInAndVerifiedFree;
  if (!currentUserIsVerifiedFree) {
    removeBanner();
  }
}

export function shouldShowAds(): boolean {
  return native && currentUserIsVerifiedFree;
}

function attachDiagnosticListeners(): void {
  if (!native) return;

  // Banner logs
  AdMob.addListener(BannerAdPluginEvents.Loaded, () => {
    console.log('[ads][banner] loaded', { adId: ADS.banner, testing: ADS.testing });
  }).catch(() => {});
  AdMob.addListener(BannerAdPluginEvents.FailedToLoad, (err: AdMobError) => {
    console.warn('[ads][banner] failed to load', { code: err?.code, message: err?.message });
  }).catch(() => {});
  AdMob.addListener(BannerAdPluginEvents.Opened, () => {
    console.log('[ads][banner] shown/opened');
  }).catch(() => {});

  // Interstitial logs
  AdMob.addListener(InterstitialAdPluginEvents.Loaded, () => {
    console.log('[ads][interstitial] loaded', { adId: ADS.interstitial, testing: ADS.testing });
  }).catch(() => {});
  AdMob.addListener(InterstitialAdPluginEvents.FailedToLoad, (err: AdMobError) => {
    console.warn('[ads][interstitial] failed to load', { code: err?.code, message: err?.message });
  }).catch(() => {});
  AdMob.addListener(InterstitialAdPluginEvents.Showed, () => {
    console.log('[ads][interstitial] shown');
  }).catch(() => {});

  // Rewarded logs
  AdMob.addListener(RewardAdPluginEvents.Loaded, () => {
    console.log('[ads][rewarded] loaded', { adId: ADS.rewarded, testing: ADS.testing });
  }).catch(() => {});
  AdMob.addListener(RewardAdPluginEvents.FailedToLoad, (err: AdMobError) => {
    console.warn('[ads][rewarded] failed to load', { code: err?.code, message: err?.message });
  }).catch(() => {});
  AdMob.addListener(RewardAdPluginEvents.Showed, () => {
    console.log('[ads][rewarded] shown');
  }).catch(() => {});

  // Rewarded Interstitial logs (if supported by the installed plugin version)
  if (RewardInterstitialAdPluginEvents) {
    AdMob.addListener(RewardInterstitialAdPluginEvents.Loaded, () => {
      console.log('[ads][rewardedInterstitial] loaded', {
        adId: ADS.rewardedInterstitial,
        testing: ADS.testing,
      });
    }).catch(() => {});
    AdMob.addListener(RewardInterstitialAdPluginEvents.FailedToLoad, (err: AdMobError) => {
      console.warn('[ads][rewardedInterstitial] failed to load', {
        code: err?.code,
        message: err?.message,
      });
    }).catch(() => {});
    AdMob.addListener(RewardInterstitialAdPluginEvents.Showed, () => {
      console.log('[ads][rewardedInterstitial] shown');
    }).catch(() => {});
  }
}

// 1) INIT + CONSENT (call once at app start, on native only)
export async function initAds(): Promise<void> {
  if (!native || ready) return;
  try {
    const info = await AdMob.requestConsentInfo();
    if (info.isConsentFormAvailable && info.status === AdmobConsentStatus.REQUIRED) {
      await AdMob.showConsentForm();
    }
    await AdMob.initialize({});
    ready = true;
    attachDiagnosticListeners();
    await Promise.allSettled([
      prepareInterstitial(),
      prepareRewarded(),
      prepareRewardedInterstitial(),
    ]);
  } catch (e) {
    console.warn('[ads] init failed', e);
  }
}

// 2) BANNER (native overlay: set bottom margin = height of bottom navigation bar in px)
export async function showBanner(bottomMarginPx: number): Promise<void> {
  if (!native || !ready || !shouldShowAds()) return;
  try {
    await AdMob.showBanner({
      adId: ADS.banner,
      adSize: BannerAdSize.ADAPTIVE_BANNER,
      position: BannerAdPosition.BOTTOM_CENTER,
      margin: bottomMarginPx,
      isTesting: ADS.testing,
    });
    console.log('[ads][banner] showBanner requested', { margin: bottomMarginPx });
  } catch (e) {
    console.warn('[ads] banner failed', e);
  }
}

export const hideBanner = (): void => {
  if (native) {
    AdMob.hideBanner().catch(() => {});
  }
};

export const removeBanner = (): void => {
  if (native) {
    AdMob.removeBanner().catch(() => {});
  }
};

// 3) INTERSTITIAL (max 1 per 3 minutes, 3 per session)
async function prepareInterstitial(): Promise<void> {
  if (!native || !ready) return;
  try {
    await AdMob.prepareInterstitial({ adId: ADS.interstitial, isTesting: ADS.testing });
  } catch (e) {
    console.warn('[ads][interstitial] prepare failed', e);
  }
}

export async function maybeShowInterstitial(): Promise<void> {
  if (!native || !ready || !shouldShowAds()) return;
  if (Date.now() - lastInterstitial < 180000 || interstitialsThisSession >= 3) return;
  try {
    await AdMob.showInterstitial();
    lastInterstitial = Date.now();
    interstitialsThisSession++;
  } catch (e) {
    console.warn('[ads][interstitial] show failed', e);
  } finally {
    prepareInterstitial();
  }
}

// 4) REWARDED (returns true ONLY if the user earned the reward)
async function prepareRewarded(): Promise<void> {
  if (!native || !ready) return;
  try {
    await AdMob.prepareRewardVideoAd({ adId: ADS.rewarded, isTesting: ADS.testing });
  } catch (e) {
    console.warn('[ads][rewarded] prepare failed', e);
  }
}

export async function showRewarded(): Promise<boolean> {
  if (!native || !ready || !shouldShowAds()) return false;
  return new Promise<boolean>(async (resolve) => {
    let earned = false;
    let settled = false;
    const subs: { remove: () => Promise<void> | void }[] = [];
    const finish = (v: boolean) => {
      if (settled) return;
      settled = true;
      subs.forEach((s) => {
        try {
          s.remove();
        } catch {}
      });
      prepareRewarded();
      resolve(v);
    };
    try {
      subs.push(
        await AdMob.addListener(RewardAdPluginEvents.Rewarded, (reward) => {
          console.log('[ads][rewarded] reward earned', reward);
          earned = true;
        })
      );
      subs.push(
        await AdMob.addListener(RewardAdPluginEvents.Dismissed, () => {
          console.log('[ads][rewarded] dismissed', { earned });
          finish(earned);
        })
      );
      subs.push(
        await AdMob.addListener(RewardAdPluginEvents.FailedToShow, (err: AdMobError) => {
          console.warn('[ads][rewarded] failed to show', {
            code: err?.code,
            message: err?.message,
          });
          finish(false);
        })
      );
      await AdMob.showRewardVideoAd();
    } catch (e) {
      console.warn('[ads][rewarded] show exception', e);
      finish(false);
    }
  });
}

// 5) REWARDED INTERSTITIAL (same pattern as rewarded; safely falls back to false if plugin version omits it)
async function prepareRewardedInterstitial(): Promise<void> {
  if (!native || !ready) return;
  const anyAdMob = AdMob as unknown as Record<string, ((opts: unknown) => Promise<unknown>) | undefined>;
  if (typeof anyAdMob.prepareRewardInterstitialAd !== 'function') return;
  try {
    await anyAdMob.prepareRewardInterstitialAd({
      adId: ADS.rewardedInterstitial,
      isTesting: ADS.testing,
    });
  } catch (e) {
    console.warn('[ads][rewardedInterstitial] prepare failed', e);
  }
}

export async function showRewardedInterstitial(): Promise<boolean> {
  if (!native || !ready || !shouldShowAds()) return false;
  const anyAdMob = AdMob as unknown as Record<string, ((opts?: unknown) => Promise<unknown>) | undefined>;
  if (
    typeof anyAdMob.showRewardInterstitialAd !== 'function' ||
    !RewardInterstitialAdPluginEvents
  ) {
    console.warn('[ads][rewardedInterstitial] not supported in installed plugin version');
    return false;
  }
  return new Promise<boolean>(async (resolve) => {
    let earned = false;
    let settled = false;
    const subs: { remove: () => Promise<void> | void }[] = [];
    const finish = (v: boolean) => {
      if (settled) return;
      settled = true;
      subs.forEach((s) => {
        try {
          s.remove();
        } catch {}
      });
      prepareRewardedInterstitial();
      resolve(v);
    };
    try {
      subs.push(
        await AdMob.addListener(RewardInterstitialAdPluginEvents.Rewarded, (reward) => {
          console.log('[ads][rewardedInterstitial] reward earned', reward);
          earned = true;
        })
      );
      subs.push(
        await AdMob.addListener(RewardInterstitialAdPluginEvents.Dismissed, () => {
          console.log('[ads][rewardedInterstitial] dismissed', { earned });
          finish(earned);
        })
      );
      subs.push(
        await AdMob.addListener(
          RewardInterstitialAdPluginEvents.FailedToShow,
          (err: AdMobError) => {
            console.warn('[ads][rewardedInterstitial] failed to show', {
              code: err?.code,
              message: err?.message,
            });
            finish(false);
          }
        )
      );
      await anyAdMob.showRewardInterstitialAd();
    } catch (e) {
      console.warn('[ads][rewardedInterstitial] show exception', e);
      finish(false);
    }
  });
}
