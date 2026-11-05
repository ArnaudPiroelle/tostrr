package app.tostrr.domain.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionsTest {
    @Test fun `a higher number is newer, compared as numbers`() {
        assertTrue(AppVersions.isNewer("0.2.0", "0.1.0"))
        assertTrue(AppVersions.isNewer("0.10.0", "0.9.0"))
        assertTrue(AppVersions.isNewer("1.0.0", "0.9.9"))
    }

    @Test fun `the same or a lower version is not newer`() {
        assertFalse(AppVersions.isNewer("0.1.0", "0.1.0"))
        assertFalse(AppVersions.isNewer("0.1.0", "0.2.0"))
        assertFalse(AppVersions.isNewer("1.0", "1.0.0"))
    }

    @Test fun `a final version comes after its pre-release, not before`() {
        assertTrue(AppVersions.isNewer("1.2.0", "1.2.0-beta.1"))
        assertFalse(AppVersions.isNewer("1.2.0-beta.1", "1.2.0"))
        assertFalse(AppVersions.isNewer("1.1.0", "1.2.0-beta.1"))
    }

    @Test fun `a name that can't be read is never newer`() {
        assertFalse(AppVersions.isNewer("latest", "0.1.0"))
        assertFalse(AppVersions.isNewer("0.2.0", ""))
    }
}
