package no.dependent_implementation;

public class Result<T> {
	public final T val;
	public final Exception error;
	private Result(T val, Exception error){
		this.val=val;
		this.error=error;
	}
	
	public static <T> Result<T> res(T val){
		return new Result<T>(val, null);
	}
	public static <T> Result<T> error(Exception error){
		return new Result<T>(null, error);
	}
	
	public boolean success(){
		return val!=null && error==null;
	}
}
