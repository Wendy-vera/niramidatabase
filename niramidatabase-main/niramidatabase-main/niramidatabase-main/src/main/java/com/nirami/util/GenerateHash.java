import org.mindrot.jbcrypt.BCrypt;

public class GenerateHash {
    public static void main(String[] args) {
        String hash = BCrypt.hashpw("#Aprendiz2024", BCrypt.gensalt(12));
        System.out.println("HASH: " + hash);
    }
}
