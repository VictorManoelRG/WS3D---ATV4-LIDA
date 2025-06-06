/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package detectors;

import edu.memphis.ccrg.lida.pam.tasks.BasicDetectionAlgorithm;
import java.util.HashMap;
import java.util.Map;
import ws3dproxy.model.Thing;

/**
 *
 * @author victor
 */
public class CanCompleteLeafletDetector extends BasicDetectionAlgorithm {
    
    private final String modality = "";
    private Map<String, Object> detectorParams = new HashMap<>();
    
    @Override
    public void init() {
        super.init();
        detectorParams.put("mode", "canCompleteLeaflet");
    }

    @Override
    public double detect() {
        String canComplete =(String)sensoryMemory.getSensoryContent(modality, detectorParams);
        
        double activation;
        if(canComplete.equals("yes")){
            activation=1.0;
        }
        else{
            activation = 0.0;
        }

        return activation;
    }
    
}
