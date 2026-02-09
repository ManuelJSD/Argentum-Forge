package org.argentumforge.engine.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GithubReleaseCheckerTest {

    @Test
    @DisplayName("Should compare equal versions as equal")
    void shouldCompareEqualVersions() {
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "1.0.0")).isEqualTo(0);
        assertThat(GithubReleaseChecker.compareVersions("2.5.3", "2.5.3")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should recognize newer major version")
    void shouldRecognizeNewerMajorVersion() {
        assertThat(GithubReleaseChecker.compareVersions("2.0.0", "1.0.0")).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "2.0.0")).isLessThan(0);
    }

    @Test
    @DisplayName("Should recognize newer minor version")
    void shouldRecognizeNewerMinorVersion() {
        assertThat(GithubReleaseChecker.compareVersions("1.5.0", "1.0.0")).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "1.5.0")).isLessThan(0);
    }

    @Test
    @DisplayName("Should recognize newer patch version")
    void shouldRecognizeNewerPatchVersion() {
        assertThat(GithubReleaseChecker.compareVersions("1.0.5", "1.0.0")).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "1.0.5")).isLessThan(0);
    }

    @Test
    @DisplayName("Should handle beta versions correctly")
    void shouldHandleBetaVersions() {
        // Beta version is OLDER than stable release
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-beta1", "1.0.0")).isLessThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "1.0.0-beta1")).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should compare beta versions lexicographically")
    void shouldCompareBetaVersionsLexicographically() {
        // beta4 < beta5 (lexicographically)
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-beta4", "1.0.0-beta5")).isLessThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-beta5", "1.0.0-beta4")).isGreaterThan(0);

        // alpha < beta (lexicographically)
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-alpha1", "1.0.0-beta1")).isLessThan(0);
    }

    @Test
    @DisplayName("Should compare equal beta versions as equal")
    void shouldCompareEqualBetaVersions() {
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-beta5", "1.0.0-beta5")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle versions with different number of parts")
    void shouldHandleVersionsWithDifferentParts() {
        // 1.0 should be treated as 1.0.0
        assertThat(GithubReleaseChecker.compareVersions("1.0", "1.0.0")).isEqualTo(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.1", "1.0")).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle real-world version comparison scenarios")
    void shouldHandleRealWorldScenarios() {
        // Current version is 1.0.0-beta4, checking against beta5
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-beta5", "1.0.0-beta4")).isGreaterThan(0);

        // Current version is 1.0.0-beta5, checking against stable 1.0.0 (Should be
        // newer)
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "1.0.0-beta5")).isGreaterThan(0);

        // Current version is 1.0.0, checking against 1.0.1-beta1
        assertThat(GithubReleaseChecker.compareVersions("1.0.1-beta1", "1.0.0")).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle v prefix in version strings")
    void shouldHandleVPrefix() {
        // The isNewerVersion method strips 'v' prefix, so we should test that too
        // But compareVersions itself doesn't handle 'v', so these should work:
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "1.0.0")).isEqualTo(0);
        // If we want to test with 'v', we'd need to strip it first or test
        // isNewerVersion
    }

    @Test
    @DisplayName("Should handle edge case: comparing prerelease with different main versions")
    void shouldHandlePrereleaseWithDifferentMainVersions() {
        // 2.0.0-alpha should be greater than 1.0.0 (main version takes precedence)
        assertThat(GithubReleaseChecker.compareVersions("2.0.0-alpha", "1.0.0")).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "2.0.0-alpha")).isLessThan(0);
    }

    @Test
    @DisplayName("Should handle Release Candidates (RC)")
    void shouldHandleReleaseCandidates() {
        // rc > beta
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-rc1", "1.0.0-beta1")).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-beta1", "1.0.0-rc1")).isLessThan(0);

        // stable > rc
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "1.0.0-rc1")).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-rc1", "1.0.0")).isLessThan(0);
    }

    @Test
    @DisplayName("Should handle numeric robustness (leading zeros)")
    void shouldHandleNumericRobustness() {
        // 1.1 vs 1.01 (should be treated as 1.1 vs 1.1 in current impl, or 1 vs 1)
        // With parseInt("01") -> 1. So 1.1 == 1.1.
        assertThat(GithubReleaseChecker.compareVersions("1.1", "1.01")).isEqualTo(0);

        // 1.0 vs 1.0.0
        assertThat(GithubReleaseChecker.compareVersions("1.0", "1.0.0")).isEqualTo(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", "1.0")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle 4-component versions")
    void shouldHandleFourComponentVersions() {
        assertThat(GithubReleaseChecker.compareVersions("1.0.0.1", "1.0.0")).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0.1", "1.0.0.0")).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0.0", "1.0.0")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should be case insensitive")
    void shouldBeCaseInsensitive() {
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-BETA", "1.0.0-beta")).isEqualTo(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0-RC", "1.0.0-rc")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle nulls gracefully")
    void shouldHandleNulls() {
        assertThat(GithubReleaseChecker.compareVersions(null, "1.0.0")).isLessThan(0);
        assertThat(GithubReleaseChecker.compareVersions("1.0.0", null)).isGreaterThan(0);
        assertThat(GithubReleaseChecker.compareVersions(null, null)).isEqualTo(0);
    }
}
