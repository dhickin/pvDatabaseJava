package org.epics.pvdatabase.example;

import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StandardFieldFactory;
import org.epics.pvdata.pv.FieldBuilder;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVStructureArray;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StandardField;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.StructureArrayData;
import org.epics.pvaccess.server.rpc.RPCRequestException;
import org.epics.pvaccess.server.rpc.RPCService;
import org.epics.pvdatabase.PVDatabase;
import org.epics.pvdatabase.PVDatabaseFactory;
import org.epics.pvdatabase.PVRecord;


/**
 * @author Dave Hickin
 *
 */
public class ExampleRPCRecord extends PVRecord {
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final StandardField standardField = StandardFieldFactory.getStandardField();

    private PVStructure pvStructure;
    private PVDouble    pvx;
    private PVDouble    pvy;
    private boolean     underControl = false;

    synchronized boolean takeControl() {
        if (!underControl) {
            underControl = true;
            return true;
        }
        return false;
    }

    synchronized void releaseControl() {
        underControl = false;
    }

    static class RPCServiceImpl implements RPCService {

        private ExampleRPCRecord pvRecord;

        RPCServiceImpl(ExampleRPCRecord record) {
                pvRecord = record;
        }

	    public PVStructure request(PVStructure args) throws RPCRequestException
        {
            boolean haveControl = pvRecord.takeControl();
            if (!haveControl)
                throw new RPCRequestException(StatusType.ERROR,
                "I'm busy");

            PVStructureArray valueField = args.getSubField(PVStructureArray.class,
                  "value");
            if (valueField == null)
                throw new RPCRequestException(StatusType.ERROR,
                    "No structure array value field");

            Structure valueFieldStructure = valueField.
                getStructureArray().getStructure();

            Scalar xField = valueFieldStructure.getField(Scalar.class, "x");
            if (xField == null || xField.getScalarType() != ScalarType.pvDouble)
                 throw new RPCRequestException(StatusType.ERROR,
                    "value field's structure has no double field x");

            Scalar yField = valueFieldStructure.getField(Scalar.class, "y");
            if (yField == null || yField.getScalarType() != ScalarType.pvDouble)
                 throw new RPCRequestException(StatusType.ERROR,
                    "value field's structure has no double field y");

            int length = valueField.getLength();
            StructureArrayData sad = new StructureArrayData();
            valueField.get(0, length, sad);
        
            for (int i = 0; i < length; i++)
            {
        	    double x = sad.data[i].getSubField(PVDouble.class, "x").get();
        	    double y = sad.data[i].getSubField(PVDouble.class, "y").get();
                pvRecord.put(x,y);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    throw new RPCRequestException(StatusType.ERROR,
                       "Error in thread sleeping");
                }
            }

            Structure topStructure = fieldCreate.createFieldBuilder().
                createStructure();

            PVStructure returned = pvDataCreate.createPVStructure(topStructure);
            pvRecord.releaseControl();
            return returned;
        }
    }

    public static ExampleRPCRecord create(String recordName)
    {
        FieldBuilder fb = fieldCreate.createFieldBuilder();
        Structure structure = fb.
            add("x",ScalarType.pvDouble).
            add("y",ScalarType.pvDouble).
            add("timeStamp",standardField.timeStamp()).
            createStructure();
       ExampleRPCRecord pvRecord = new ExampleRPCRecord(recordName, pvDataCreate.createPVStructure(structure));
       PVDatabase master = PVDatabaseFactory.getMaster();
       master.addRecord(pvRecord);
       return pvRecord;
    }

    public ExampleRPCRecord(String recordName, PVStructure pvStructure) {
        super(recordName, pvStructure);
        this.pvStructure = pvStructure;
        this.pvx = pvStructure.getSubField(PVDouble.class, "x");
        this.pvy = pvStructure.getSubField(PVDouble.class, "y");
        process();
    }

    private void put(double x, double y)
    {
        lock();
        beginGroupPut();
        pvx.put(x);
        pvy.put(y);
        process();
        endGroupPut();
        unlock();
    }

    public RPCService getService(PVStructure pvRequest)
    {
        return new RPCServiceImpl(this);
    }
}