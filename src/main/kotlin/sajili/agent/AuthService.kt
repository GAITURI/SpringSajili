package sajili.agent

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import org.springframework.stereotype.Service


//    apparently a service layer for user authentication, applying firebase
@Service
class AuthService (
    //inject the firebaseAuth bean configured in the FirebaseConfig class
    private val firebaseAuth: FirebaseAuth){

//      verifies a Firebase Id token and returns the user's UID
//    This is the single entry point for backend authentication with Firebase
// @param:idToken The Firebase ID token received from the client
//    @return: the unique user id(UID) of the authenticated user.
//    @throws: FirebaseAuthException if the token is invalid or expired.

    fun verifyFirebaseIdToken(idToken:String):String{
        return try {
//            now the verifyTokenId handles the heavylifting
//            it checks the token signature(valid or invalid)
//            it verifies the token's expiration time
//            it also checks that the token was issued by firebase  cool right!! through a (return try with a catch function)!!
            val decodedToken = firebaseAuth.verifyIdToken(idToken)
//            if verification is successful, returns the user's UID
            decodedToken.uid
//            fun fact the .uid
//             is a decoded and verified Firebase token.
        //    Can be used to get the uid and other user attributes available in the token.

        }catch (e: FirebaseAuthException){
//            we are to write logs the error for backend debugging
            println("Firebase token verification failed:${e.message}")

    throw e
     }

    }

}