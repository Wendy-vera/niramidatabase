import org.mindrot.jbcrypt.BCrypt;

public class GenerateHash {
    public static void main(String[] args) {
        String hash = BCrypt.hashpw("RosaFabio9014", BCrypt.gensalt(12));
        System.out.println("HASH: " + hash);
    }
}
