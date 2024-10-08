
package org.cloudsimplus.examples;

import ch.qos.logback.classic.Level;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.cloudsimplus.util.TimeUtil.elapsedSeconds;
import static org.cloudsimplus.util.TimeUtil.secondsToStr;


public class LargeScaleExample {
    private static final int  HOSTS = 200_000;
    private static final int  HOST_PES = 16;
    private static final int  HOST_MIPS = 1000;
    private static final int  HOST_RAM = 2048; //in Megabytes
    private static final long HOST_BW = 10_000; //in Megabits/s
    private static final long HOST_STORAGE = 1_000_000; //in Megabytes

    private static final int VMS = HOSTS*4;
    private static final int VM_PES = HOST_PES/4;

    private static final int CLOUDLETS = VMS;
    private static final int CLOUDLET_PES = VM_PES/2;
    private static final int CLOUDLET_LENGTH = HOST_MIPS * 10;

    /**
     * Defines a time interval to process cloudlets execution
     * and possibly collect data. Setting a value greater than 0
     * enables that interval, which cause huge performance penaults for
     * lage scale simulations.
     *
     * @see Datacenter#setSchedulingInterval(double)
     */
    private static final double SCHEDULING_INTERVAL = -1;

    private final CloudSimPlus simulation;
    private final DatacenterBroker broker0;
    private final List<Vm> vmList;
    private final List<Cloudlet> cloudletList;
    private final Datacenter datacenter0;
    private final double startSecs;

    public static void main(String[] args) {
        new LargeScaleExample();
    }

    private LargeScaleExample() {
        // Disable logging for performance improvements.
        Log.setLevel(Level.OFF);

        this.startSecs = System.currentTimeMillis()/1000.0;
        System.out.println("Creating simulation scenario at " + LocalDateTime.now());
        System.out.printf("Creating 1 Datacenter -> Hosts: %,d VMs: %,d Cloudlets: %,d%n", HOSTS, VMS, CLOUDLETS);

        simulation = new CloudSimPlus();
        datacenter0 = createDatacenter();

        //Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerSimple(simulation);

        vmList = createVms();
        cloudletList = createCloudlets();
        brokerSubmit();

        System.out.println("Starting simulation after " + actualElapsedTime());
        simulation.start();

        final long submittedCloudlets = broker0.getCloudletSubmittedList().size();
        final long cloudletFinishedList = broker0.getCloudletFinishedList().size();
        System.out.printf("Submitted Cloudlets: %d Finished Cloudlets: %d%n", submittedCloudlets, cloudletFinishedList);

        System.out.printf(
            "Simulated time: %s Actual Execution Time: %s%n", simulatedTime(), actualElapsedTime());
    }

    private String simulatedTime() {
        return secondsToStr(simulation.clock());
    }

    private String actualElapsedTime() {
        return secondsToStr(elapsedSeconds(startSecs));
    }

    private void brokerSubmit() {
        System.out.printf("Submitting %,d VMs%n", VMS);
        broker0.submitVmList(vmList);

        System.out.printf("Submitting %,d Cloudlets%n", CLOUDLETS);
        broker0.submitCloudletList(cloudletList);
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private Datacenter createDatacenter() {
        final var hostList = new ArrayList<Host>(HOSTS);
        System.out.printf("Creating %,d Hosts%n", HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            final var host = createHost();
            hostList.add(host);
        }

        var dc = new DatacenterSimple(simulation, hostList, new VmAllocationPolicyFirstFit());
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    private Host createHost() {
        final var peList = new ArrayList<Pe>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(HOST_MIPS));
        }

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        return new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        final var vmList = new ArrayList<Vm>(VMS);
        System.out.printf("Creating %,d VMs%n", VMS);
        for (int i = 0; i < VMS; i++) {
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
            final var vm = new VmSimple(HOST_MIPS, VM_PES);
            vm.setRam(512).setBw(1000).setSize(10_000);
            vmList.add(vm);
        }

        return vmList;
    }

    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        final var cloudletList = new ArrayList<Cloudlet>(CLOUDLETS);

        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        final var utilizationModel = new UtilizationModelDynamic(0.5);

        System.out.printf("Creating %,d Cloudlets%n", CLOUDLETS);
        for (int i = 0; i < CLOUDLETS; i++) {
            final var cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel);
            cloudlet.setSizes(1024);
            cloudletList.add(cloudlet);
        }

        return cloudletList;
    }
}
