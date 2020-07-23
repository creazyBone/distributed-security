public class Child extends Parent {

    protected void hello(){
        System.out.println("child");
    }
    public void seeParentName(){
        System.out.println(super.getName());
    }
}
