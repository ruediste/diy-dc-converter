import { capacitanceUnits, CurrentInput, currentUnits, CurrentValue, formatValue, frequencyUnits, InductanceInput, InductanceValue, NumberInput, powerUnits, RangeIndicator, resistanceUnits, round, timeUnits, VoltageInput, VoltageValue } from "./Input";
import { useMapLocalStorage } from "./useLocalStorage"

interface State {
    inputVoltage: VoltageValue
    outputVoltage: VoltageValue
    inductance: InductanceValue
    idlePercentage: number
    loadCurrent: CurrentValue
    inputRipple: VoltageValue
    outputRipple: VoltageValue
    currentGain: number
}

export function FirstConverter() {
    return useMapLocalStorage<State>('firstConverter', {
        inputVoltage: { value: 5, unit: 'V' },
        outputVoltage: { value: 12, unit: 'V' },
        inputRipple: { value: 50e-3, unit: 'mV' },
        outputRipple: { value: 50e-3, unit: 'mV' },
        inductance: { value: 1e-3, unit: 'mH' },
        idlePercentage: 10,
        loadCurrent: { value: 0.01, unit: 'mA' },
        currentGain: 50
    }, (stateAccess) => {
        const [state] = stateAccess;
        const idleFraction = state.idlePercentage / 100;

        const k = (state.outputVoltage.value - state.inputVoltage.value) / state.inputVoltage.value;
        const peakCurrent = 2 * (k + 1) * state.loadCurrent.value / (1 - idleFraction);
        const chargeTime = state.inductance.value * peakCurrent / state.inputVoltage.value;
        const dischargeTime = state.inductance.value * peakCurrent / (state.outputVoltage.value - state.inputVoltage.value);
        const cycleTime = (chargeTime + dischargeTime) / (1 - idleFraction);
        const switchingFrequency = 1 / cycleTime;
        const loadResistor = state.outputVoltage.value / state.loadCurrent.value;
        const outputPower = state.outputVoltage.value * state.loadCurrent.value;
        const outputCapacitance = peakCurrent * dischargeTime / (2 * state.outputRipple.value);
        const inputCapacitance = peakCurrent * chargeTime / (2 * state.inputRipple.value);
        const baseCurrent = peakCurrent / state.currentGain;
        const baseResistor = (3.3 - 0.6) / baseCurrent;

        return <div className="toolContainer">
            <div className="row">
                <VoltageInput stateAccess={stateAccess} k="inputVoltage" className="col" />
                <VoltageInput stateAccess={stateAccess} k="outputVoltage" className="col" />
            </div>
            <div className="row">
                <InductanceInput stateAccess={stateAccess} k="inductance" className="col" />
                <CurrentInput stateAccess={stateAccess} k="loadCurrent" className="col" />
            </div>
            <div className="row">
                <NumberInput stateAccess={stateAccess} k="idlePercentage" unit="%" className="col" />
                <NumberInput stateAccess={stateAccess} k="currentGain" className="col" label="Transistor Current Gain" />
            </div>
            <div className="row">
                <VoltageInput stateAccess={stateAccess} k="inputRipple" className="col" />
                <VoltageInput stateAccess={stateAccess} k="outputRipple" className="col" />
            </div>
            <div>
                Charge Time: {formatValue(chargeTime, timeUnits)}
                <RangeIndicator log value={chargeTime} min={1e-6} max={1e-3} units={timeUnits}
                    minWarning="Try to use a larger inductor" maxWarning="Try to use a smaller inductor" />
            </div>
            <div>
                Discharge Time: {formatValue(dischargeTime, timeUnits)}
                <RangeIndicator log value={dischargeTime} min={1e-6} max={1e-3} units={timeUnits}
                    minWarning="Try to use a larger inductor" maxWarning="Try to use a smaller inductor" />
            </div>
            <div>
                Switching Frequency: {formatValue(switchingFrequency, frequencyUnits)}
                <RangeIndicator log value={switchingFrequency} min={5e3} max={200e3} maxWarning="The frequency is rather high" units={frequencyUnits} />
            </div>
            <div> Peak Inductor Current: {formatValue(peakCurrent, currentUnits)}
                <RangeIndicator value={peakCurrent} min={1e-3} max={1} maxWarning="Make sure your inductor and your transistor can handle this current" units={currentUnits} />
            </div>
            <div className="row">
                <div className="col col-3"> Load Resistor: {formatValue(loadResistor, resistanceUnits)} </div>
                <div className="col"> Output Power: {formatValue(outputPower, powerUnits)}
                    <RangeIndicator value={outputPower} min={1e-3} max={250e-3} maxWarning="Typical small resistors are rated for 1/8W or 1/4W. Make sure to use an adequate resistor" units={powerUnits} />
                </div>
            </div>
            <div> Base Current: {formatValue(baseCurrent, currentUnits)}
                <RangeIndicator value={baseCurrent} min={0} max={20e-3} maxWarning="The datasheet of the STM32F401 indicates a max current of 25mA. Don't exceed it" units={currentUnits} />
            </div>
            <div className="row">
                <div className="col">
                    <div> Duty: {round(chargeTime / cycleTime * 100, 2)}% </div>
                    <div> Base Resistor: {formatValue(baseResistor, resistanceUnits)} </div>
                </div>
                <div className="col">
                    <div> Output Capacitor: {formatValue(outputCapacitance, capacitanceUnits)} </div>
                    <div> Input Capacitor: {formatValue(inputCapacitance, capacitanceUnits)} </div>
                </div>
            </div>
            <div>
                <button type="button" className="btn btn-secondary" onClick={stateAccess[2]}>Reset</button>
            </div >
        </div >
    });
}