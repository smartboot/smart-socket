import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/21
 */
public class Demo {

    public static void main(String[] args) {
        Map<Integer, String> map = new HashMap<>();

        map.put(1, ".----");
        map.put(2, "..---");
        map.put(3, "...--");
        map.put(4, "....-");
        map.put(5, ".....");
        map.put(6, "-....");
        map.put(7, "--...");
        map.put(8, "---..");
        map.put(9, "----.");
        map.put(0, "-----");
        System.out.println("编码规则");
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("示例");
        String num = "1566202300863154";
        char[] array = num.toCharArray();
        for (char c : array) {
            System.out.print(c + ": ");
            for (int i = 0; i < 5; i++) {
                System.out.print(map.get(Integer.parseInt(c + "")));
            }
            System.out.println();
        }
    }
}
