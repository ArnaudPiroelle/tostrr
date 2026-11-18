package app.tostrr.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.koin.core.annotation.Single
import java.io.File

/** Whether a downloaded APK is a build of this very app, signed with the key of the installed one. */
fun interface ApkCheck {
    fun isUpdateOfThisApp(apk: File, version: String): Boolean
}

/**
 * Reads the APK's package name, version name and signers: Android would refuse another signer over the installed app,
 * but would install another package beside it, as a new app the user believes to be Tostrr's update.
 */
@Single
class InstalledAppApkCheck(private val context: Context) : ApkCheck {
    override fun isUpdateOfThisApp(apk: File, version: String): Boolean {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(apk.path, SIGNERS) ?: return false
        if (archive.packageName != context.packageName || archive.versionName != version) return false
        val installed = pm.getPackageInfo(context.packageName, SIGNERS)
        val signers = archive.signers()
        return signers.isNotEmpty() && signers == installed.signers()
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.signers(): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.let { if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory }
        } else {
            signatures
        }
        return signatures.orEmpty().map { it.toCharsString() }.toSet()
    }

    private companion object {
        @Suppress("DEPRECATION")
        val SIGNERS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    }
}
