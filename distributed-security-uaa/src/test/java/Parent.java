import java.util.HashMap;

public class Parent {
    private String name = "aaa";

    private HashMap<String,String> map = new HashMap<>();

    public void setName(String name){

        this.name = name;
        System.out.println("setName-->this:"+this+"name:"+this.name);
    }

    public String getName(){
        System.out.println("getName-->this:"+this+"name:"+this.name);
        this.hello();
        return this.name;
    }

    protected void hello(){
        System.out.println("parent");
    }

    public void sayHello(){
        this.hello();
    }

    public void put(String key,String value){
        this.map.put(key,value);
    }

    public String get(String key){
      return this.map.get(key);
    }
}
