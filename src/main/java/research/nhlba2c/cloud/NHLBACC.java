package research.nhlba2c.cloud;

import ch.qos.logback.classic.Level;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicy;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;

import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;

import org.cloudsimplus.core.CloudSimPlus;

import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;

import org.cloudsimplus.distributions.ContinuousDistribution;
import org.cloudsimplus.distributions.UniformDistr;

import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;

import org.cloudsimplus.provisioners.ResourceProvisionerSimple;

import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;

import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;

import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;

import org.cloudsimplus.util.Log;
import org.cloudsimplus.util.TimeUtil;

import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;

import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.time.LocalDateTime;
import java.util.*;

import static org.cloudsimplus.util.TimeUtil.elapsedSeconds;
import static org.cloudsimplus.util.TimeUtil.secondsToStr;


public class NHLBACC {
	private static List<Integer> possible_HostPES = Arrays.asList(4, 8, 16, 32, 64);
	private static List<Integer> possible_CloudletLengths = Arrays.asList(3000, 5000, 7000, 10000);
	
	private static Random rand = new Random();
    private static final int HOSTS = 200;
    private static final int HOST_PES = possible_HostPES.get(rand.nextInt(possible_HostPES.size()));
    
    private static final int PE_power = 2500; //was set to 1000
    private static final int HOST_RAM = 16384;
    private static final int HOST_BW=10000;
    private static final int HOST_storage=1000000;

    private static final int VMS = 300;
    private static final int VM_PES = 2;

    private static final int VM_RAM = 512;
    private static final int VM_BW = 1000;
    private static final int VM_storage = 10000;
    private static final int CLOUDLETS = 500;
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LENGTH = possible_CloudletLengths.get(rand.nextInt(possible_CloudletLengths.size()));

    private final CloudSimPlus simulation;
    private final DatacenterBroker broker0;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private Datacenter datacenter0;
	
    private final ContinuousDistribution random;
    
    //Main method
	public static void main(String[] args) {
			new NHLBACC();
	}
	
	//class constructor 
	private NHLBACC() {
		//simulation parameters
		random = new UniformDistr();
		
		//Simulation initialization
        final double startSecs = TimeUtil.currentTimeSecs();
        //Enabling only some types of log messages.
        Log.setLevel(ch.qos.logback.classic.Level.WARN);
        simulation = new CloudSimPlus();
        
        System.out.printf("Creating 1 Datacenter -> Hosts: %,d With %,d PES VMs: %,d Cloudlets: %,d%n With a size of %,d millions of instructions", HOSTS, HOST_PES,VMS, CLOUDLETS, CLOUDLET_LENGTH);
        //Building architecture components
        datacenter0 = createDatacenter();
        //Datacenter broker simple: is a simple implementation of datacenter broker which tries to place a VM into the first found datacenter
        broker0 = new DatacenterBrokerSimple(simulation);
        //Calls a function to create a set of virtual machines
        vmList = createVms();
      //Calls a function to create a set of cloudlets
        cloudletList = createCloudlets();
        
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);
        //System.out.println(datacenter0.getHostList());
        //Launching simulation and getting output
        simulation.start();
        final var cloudletFinishedList = broker0.getCloudletFinishedList();
        cloudletFinishedList.sort(Comparator.comparingLong(cloudlet -> cloudlet.getVm().getId()));
        new CloudletsTableBuilder(cloudletFinishedList).build();
        System.out.println("Execution time: " + TimeUtil.secondsToStr(TimeUtil.elapsedSeconds(startSecs)));
	}
	
	//Datacenter creation method
	private Datacenter createDatacenter() {
        final var hostList = new ArrayList<Host>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            final var host = createHost();
            hostList.add(host);
        }

        final var vmAllocationPolicy = new VmAllocationPolicySimple();

        //Replaces the default method that allocates Hosts to VMs by our own implementation
        vmAllocationPolicy.setFindHostForVmFunction(this::findSuitableHostForVm);

        return new DatacenterSimple(simulation, hostList, vmAllocationPolicy);
	}
	
    private Optional<Host> findSuitableHostForVm(final VmAllocationPolicy vmAllocationPolicy, final Vm vm) {
        final var hostList = vmAllocationPolicy.getHostList();
        /* Despite the loop is bound to the number of Hosts inside the List,
        *  the index "i" is not used to get a Host at that position,
        *  but just to define that the maximum number of tries to find a
        *  suitable Host will be the number of available Hosts.*/
        for (int i = 0; i < hostList.size(); i++){
            final int randomIndex = (int)(random.sample() * hostList.size());
            final Host host = hostList.get(randomIndex);
            //System.out.println(host.getTotalAvailableMips()/host.getTotalMipsCapacity()*100);         
            if(host.isSuitableForVm(vm)){
                return Optional.of(host);
            }
        }

        return Optional.empty();
    }
	
	//Host creation method 
    private Host createHost() {
        final var peList = new ArrayList<Pe>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(PE_power));
        }

        final long ram = HOST_RAM; //in Megabytes
        final long bw = HOST_BW ; //in Megabits/s
        final long storage = HOST_storage; //in Megabytes
        final var host = new HostSimple(ram, bw, storage, peList);
        host
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerTimeShared());
        return host;
    }
    
    //creation of a list of virtual machines
    private List<Vm> createVms() {
        final var list = new ArrayList<Vm>(VMS);
        for (int i = 0; i < VMS; i++) {
            Vm vm =
                new VmSimple(i, PE_power, VM_PES)
                    .setRam(VM_RAM).setBw(VM_BW).setSize(VM_storage)
                    .setCloudletScheduler(new CloudletSchedulerSpaceShared());

            list.add(vm);
        }

        return list;
    }
    
    //List of Cloudlets creation 
    private List<Cloudlet> createCloudlets() {
        final var list = new ArrayList<Cloudlet>(CLOUDLETS);
        final var utilization = new UtilizationModelDynamic(0.2);
        for (int i = 0; i < CLOUDLETS; i++) {
            final var cloudlet =
                new CloudletSimple(i, CLOUDLET_LENGTH, CLOUDLET_PES)
                    .setFileSize(1024)
                    .setOutputSize(1024)
                    .setUtilizationModelBw(utilization)
                    .setUtilizationModelRam(utilization)
                    .setUtilizationModelCpu(new UtilizationModelFull());
            list.add(cloudlet);
        }

        return list;
    }
    

	

}
