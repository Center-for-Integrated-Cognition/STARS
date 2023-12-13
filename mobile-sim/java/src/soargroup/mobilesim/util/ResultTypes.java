package soargroup.mobilesim.util;

public abstract class ResultTypes {
	/******** IsValid: Valid, NotValid *******/
	public abstract static class IsValid {
		public static Valid True(){
			return new Valid();
		}
		public static NotValid False(String reason){
			return new NotValid(reason);
		}
		public IsValid AND(IsValid other){
			if(this instanceof NotValid)
				return this;
			if(other instanceof NotValid)
				return other;
			return IsValid.True();
		}
	}

	public static class Valid extends IsValid { }
	public static class NotValid extends IsValid {
		public final String reason;
		public NotValid(String reason){
			this.reason = reason;
		}
	}

	/******** Result: Ok, Err *******/
	public static class Result {
		public static Ok Ok(){ 
			return new Ok(); 
		}
		public static Err Err(String reason){ 
			return new Err(reason); 
		}
	}

	public static class Ok extends Result { }
	public static class Err extends Result {
		public final String reason;
		public Err(String reason){
			this.reason = reason;
		}
	}
}
