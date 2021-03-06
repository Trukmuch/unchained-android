package com.github.livingwithhippos.unchained.authentication.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.repositoy.AuthenticationRepository
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UserRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass.
 * It offers LiveData to be observed during the authentication process and uses the [AuthenticationRepository] to manage its process.
 */
class AuthenticationViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val authLiveData = MutableLiveData<Event<Authentication?>>()
    val secretLiveData = MutableLiveData<Event<Secrets>>()
    val tokenLiveData = MutableLiveData<Event<Token?>>()
    val userLiveData = MutableLiveData<Event<User?>>()

    //todo: here we should check if we already have credentials and if they work, and pass those
    fun fetchAuthenticationInfo() {
        viewModelScope.launch {
            val authData = authRepository.getVerificationCode()
            authLiveData.postEvent(authData)
        }
    }

    /**
     * @param deviceCode: the device code assigned calling the authentication endpoint
     * @param expireIn: the time in seconds before the deviceCode is not valid anymore for the secrets endpoint
     */
    fun fetchSecrets(deviceCode: String, expireIn: Int) {
        // 5 seconds is the value suggested by real debrid
        val waitTime = 5000L
        // this is just an estimate, keeping track of time would be more precise. As of now this value should be 120
        var calls = (expireIn * 1000 / waitTime).toInt() - 10
        // remove 10% of the calls to account for the api calls
        calls -= calls / 10
        viewModelScope.launch {
            var secretData = authRepository.getSecrets(deviceCode)
            while (secretData?.clientId == null && calls-- > 0 && !(getAuthState() == AuthenticationState.AUTHENTICATED || getAuthState() == AuthenticationState.AUTHENTICATED_NO_PREMIUM)) {
                delay(waitTime)
                secretData = authRepository.getSecrets(deviceCode)
                calls++
            }
            if (secretData?.clientId != null) {
                secretLiveData.postEvent(secretData)
            } else {
                // if the authentication link has expired before the user confirmation, request a new one
                if (calls <= 0)
                    fetchAuthenticationInfo()
            }
        }

    }

    fun fetchToken(clientId: String, deviceCode: String, clientSecret: String) {
        viewModelScope.launch {
            val tokenData = authRepository.getToken(clientId, clientSecret, deviceCode)
            tokenLiveData.postEvent(tokenData)
            if (tokenData?.accessToken != null) {
                // i need only a set of credentials in my application
                credentialRepository.deleteAllOpenSourceCredentials()

            }
        }
    }

    fun checkAndSaveToken(privateKey: String? = null, token: Token? = null) {
        viewModelScope.launch {

            if (privateKey == null && token == null)
                throw IllegalArgumentException("checkAndSaveToken: passed tokens were both null")

            // try to get user info
            val user: User? = userRepository.getUserInfo(privateKey ?: token!!.accessToken)

            if (user != null) {
                if (privateKey != null)
                    credentialRepository.insertPrivateToken(privateKey)
                else {
                    val deviceCode = authLiveData.value?.peekContent()?.deviceCode
                    val clientId = secretLiveData.value?.peekContent()?.clientId
                    val clientSecret = secretLiveData.value?.peekContent()?.clientSecret
                    if (deviceCode != null && clientId != null && clientSecret != null)
                        credentialRepository.insert(
                            Credentials(
                                deviceCode = deviceCode,
                                clientId = clientId,
                                clientSecret = clientSecret,
                                accessToken = token!!.accessToken,
                                refreshToken = token.refreshToken
                            )
                        )
                }
            }

            // alert the observing fragment of the result
            userLiveData.postEvent(user)
        }
    }

    fun setAuthState(state: AuthenticationState) {
        savedStateHandle.set(AUTH_STATE, state)
    }

    private fun getAuthState(): AuthenticationState? {
        return savedStateHandle.get(AUTH_STATE)
    }

    companion object {
        const val AUTH_STATE = "auth_state"
    }
}