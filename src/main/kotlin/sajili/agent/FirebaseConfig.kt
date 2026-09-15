package sajili.agent

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.IOException
import java.io.InputStream


//@Configuration:marks this class(class-level annotation) as a source of bean definition for the spring application context
@Configuration
public class FirebaseConfig {

    @Bean
    @Throws(IOException::class)
//    the function below can return a FirebaseApp object
    fun firebaseApp(): FirebaseApp{
        val apps = FirebaseApp.getApps()
        if (apps.isNotEmpty()) {
            return apps[0]
        }

//       val serviceAccountResource ->creates a classpathresource to load the service account key from the classpath
        val serviceAccountResource = ClassPathResource("firebase-login-agent.json")
//      val serviceAccountStream:InputStream= serviceAccountResource.inputStream ->gets an input stream to read the content of the JSON file
        val serviceAccountStream:InputStream= serviceAccountResource.inputStream
//let's create the configuration options for the FirebaseApp
//       FirebaseOptions.builder()->starts a builder pattern to create the option object
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))->using the Input stream created this sets the Authentication credentials from the JSON file
        val options= FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
           .build() //->finalizes the creation of the FirebaseOptions object
        return  FirebaseApp.initializeApp(options) //-> returns the initialized FirebaseApp instance to be used as a Spring bean
    }
<<<<<<< HEAD

=======
    @Bean
    fun firebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth{

        return FirebaseAuth.getInstance(firebaseApp) //returns the FirebaseAuth instance as a bean
    }
>>>>>>> 03cfdcc46b34b0c5e4d17c3ca196dedeab85b2b0

}
//@Configuration- is a class level annotation
//when the spring container starts up, it looks for classes annotated with @Configuration
//these classes are special because they can define beans which are objects managed by the Spring IOC(Inversion of control) container
//@Bean :tells spring to create a managed object(a "bean") from the return value of this function.
//@Throws(IOException::class) ->its a kotlin annotation to declare that this function can throw an IOException

