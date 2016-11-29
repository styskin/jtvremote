
public class TVMessage implements Cloneable {
	
	private String url;
	private int force;
	
	public TVMessage() {
		url = "";
		force = 0;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public TVMessage clone() throws CloneNotSupportedException {
		return (TVMessage) super.clone();
	}

	public int getForce() {
		return force;
	}

	public void setForce(int force) {
		this.force = force;
	}
}
