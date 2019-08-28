import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author 三刀
 * @version V1.0 , 2019/8/22
 */
public class UnsafeDemo {
    public static void main(String[] args) throws NoSuchFieldException {
        Unsafe unsafe = getUnsafe();
        Student student = new Student("三刀", 18);
        Field nameField = Student.class.getDeclaredField("name");
        Field ageField = Student.class.getDeclaredField("age");

        long nameOffset = unsafe.objectFieldOffset(nameField);
        System.out.println("offset: " + nameOffset + " value: " + unsafe.getObject(student, nameOffset));

        long ageOffset = unsafe.objectFieldOffset(ageField);
        System.out.println("offset: " + ageOffset + " value: " + unsafe.getInt(student, ageOffset));

        unsafe.putObject(student, nameOffset, "sandao");
        System.out.println(student.getName());
//        unsafe.getInt()
    }

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            return unsafe;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

class Student {
    private String name;
    private int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}