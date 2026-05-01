import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password";
        String hashed = encoder.encode(password);
        System.out.println("HASH_START:" + hashed + ":HASH_END");
    }
}
