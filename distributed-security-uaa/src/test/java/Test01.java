public class Test01 {

    public static void main(String[] args) {
      //  Parent parent = new Parent();
     //   parent.setName("ccc");
        Child child = new Child();
  ///      ((Parent)child).sayHello();

        child.put("1","hello");
        System.out.println(child.get("1"));


      //  ((Parent)child).getName();
      //  child.getName();
       // child.setName("bbb");
  //      System.out.println(parent.getName());
    }
}
