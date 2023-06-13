package research.nhlba2c.cloud;


import java.util.List;

import org.cloudsimplus.hosts.Host;
public class Cluster {

	private int id;
	private int category;
	private List<Host> cluster_servers;
	public Cluster()
	{
		category=0;
		cluster_servers=null;
	}
	public int getCategory() {
		return category;
	}
	public void setCategory(int category) {
		this.category = category;
	}
	public List<Host> getCluster_servers() {
		return cluster_servers;
	}
	public void setCluster_servers(List<Host> cluster_servers) {
		this.cluster_servers = cluster_servers;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	
}
