public class Test02 {
    public static void main(String[] args) {
       new Thread(()->{
           ParameterHolder.map.put("1","aaa");
           try {
               Thread.sleep(3000);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(ParameterHolder.map.get("1"));
    }


}
