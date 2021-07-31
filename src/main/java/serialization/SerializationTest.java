package serialization;

import java.io.*;

public class SerializationTest {
    private static final String file = "file.txt";
    public static void main(String[] args) {
        Person person = new Person();
        person.age = 23;
        person.name = "Jeremy Zhang";
        person.height = 170;  // transient is not written

        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(person);
            objectOutputStream.flush();
            objectOutputStream.close();
            System.out.println("Write object to file finished");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
            Person personObject = (Person) objectInputStream.readObject();
            System.out.printf("Person age is: %d | name is: %s", personObject.age, personObject.name);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}



class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    static String country = "ITALY";
    public int age;
    public String name;
    transient int height;
}
