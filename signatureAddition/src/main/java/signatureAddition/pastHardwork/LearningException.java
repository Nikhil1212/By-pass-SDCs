package signatureAddition.pastHardwork;

public class LearningException {

	public static void main(String[] args) throws ArithmeticException, InterruptedException{
		// TODO Auto-generated method stub
		String str="";
		while(true)
		{
			System.out.println("Hope you are seeing me multiple times");
			try
			{
				char ch=str.charAt(-1);
				Thread.sleep(1000);
			}
			catch (Exception e) {
				// TODO: handle exception
				System.out.println(e.getMessage());
			}
			
		}
		
	}

}
