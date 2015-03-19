/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * Author(s): Pararth Shah (pararthshah717@gmail.com)
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.datasource;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;

public class ProbeDataSource extends StartableDataSource {

    @Configurable
    public Probe source;
    
    @Override
    protected void onStart() {
        source.registerListener(delegator);
        if(source instanceof Probe.Base){
            // ktkarhu: when Pipeline is initialized this gets called, through calling start probe gets also enabled and then started. Why probe is started outside Alarm cycle? Won't alarm cause a second start?
            ((Probe.Base)source).start();
        }
    }
    
    @Override
    protected void onStop() {
        // why listener (DataCollector through delegator) is unregistered for continuous probes and thus they are also disabled at stop?
        /*
        if (source instanceof ContinuousProbe) {
            ((ContinuousProbe) source).unregisterListener(delegator);
        }
        */
        if (source instanceof Probe.Base){
            ((Probe.Base)source).stop();
        }
    }
}
