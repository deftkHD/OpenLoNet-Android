package de.deftk.lonet.mobile

import android.content.Context
import android.util.Base64
import de.deftk.lonet.api.model.User
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AuthStore {

    const val PREFERENCE_NAME = "LoNetMobile_Data"

    // crypto stuff from https://stackoverflow.com/questions/13433529/android-4-2-broke-my-encrypt-decrypt-code-and-the-provided-solutions-dont-work/39002997#39002997
    // might be a bit overkill

    private const val cipherAlgorithm = "AES/CBC/PKCS5Padding"
    private val key = byteArrayOf(65, 44, 22, 64, 86, 26, 23, 111, 76, 98, 71, 84, 15, 76, 63, 24)
    private val random = SecureRandom()

    lateinit var appUser: User

    fun getSavedUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFERENCE_NAME, 0)
        if (!prefs.contains("login")) return null
        return decrypt(prefs.getString("login", null)!!)
    }

    fun getSavedToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFERENCE_NAME, 0)
        if (!prefs.contains("token")) return null
        return decrypt(prefs.getString("token", null)!!)
    }

    fun saveUsername(username: String, context: Context) {
        val prefs = context.getSharedPreferences(PREFERENCE_NAME, 0)
        prefs.edit().putString("login", encrypt(username)).apply()
    }

    fun saveToken(token: String, context: Context) {
        val prefs = context.getSharedPreferences(PREFERENCE_NAME, 0)
        prefs.edit().putString("token", encrypt(token)).apply()
    }

    private fun encrypt(str: String): String {
        val salt = getRandomBytes(32)
        val cipher = Cipher.getInstance(cipherAlgorithm)
        val iv = getRandomBytes(cipher.blockSize)
        val ivParams = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt), ivParams)
        val encrypted = cipher.doFinal(str.toByteArray(Charsets.UTF_8))
        return String.format("%s%s%s%s%s", toBase64(salt), "]", toBase64(iv), "]", toBase64(encrypted))
    }

    private fun decrypt(str: String): String {
        val fields = str.split("]")
        val salt = fromBase64(fields[0])
        val iv = fromBase64(fields[1])
        val encrypted = fromBase64(fields[2])
        val key = deriveKey(salt)
        val cipher = Cipher.getInstance(cipherAlgorithm)
        val ivParams = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivParams)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes
    }

    private fun deriveKey(salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(key.map { it.toChar() }.toTypedArray().toCharArray(), salt, 1000, 256)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun toBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun fromBase64(base64: String): ByteArray {
        return Base64.decode(base64, Base64.NO_WRAP)
    }

}