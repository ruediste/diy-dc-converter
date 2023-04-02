import { CapacitanceInput, CapacitanceValue, formatValue, FrequencyInput, FrequencyValue, NumberInput, powerUnits, RangeIndicator, ResistanceInput, resistanceUnits, ResistanceValue, round, VoltageInput, voltageUnits, VoltageValue } from "./Input";
import { useMapLocalStorage } from "./useLocalStorage"

interface State {
    switchingFrequency: FrequencyValue
    maxMeasuredVoltage: VoltageValue
    adcReferenceVoltage: VoltageValue
    lowerResistor: ResistanceValue
    adcSamplingCycles: number
    adcFrequency: FrequencyValue
    adcResolution: number
    adcCapacitance: CapacitanceValue
    adcSamplingSwitchResistance: ResistanceValue
}

export function AdcCalculator() {
    return useMapLocalStorage<State>('adcCalculator', {
        switchingFrequency: { value: 100e3, unit: 'kHz' },
        maxMeasuredVoltage: { value: 20, unit: 'V' },
        adcReferenceVoltage: { value: 3.3, unit: 'V' },
        lowerResistor: { value: 20e3, unit: 'kOhm' },
        adcSamplingCycles: 56,
        adcFrequency: { value: 21e6, unit: 'MHz' },
        adcResolution: 12,
        adcCapacitance: { value: 7e-12, unit: 'pF' },
        adcSamplingSwitchResistance: { value: 6e3, unit: 'kOhm' },
    }, (stateAccess) => {
        const [state] = stateAccess;
        const switchingPeriod = 1 / state.switchingFrequency.value;
        const adcCyclesPerPeriod = switchingPeriod / (1 / state.adcFrequency.value);
        const conversionCycles = state.adcResolution + 3;
        const adcCyclesPerMeasurement = state.adcSamplingCycles + conversionCycles;

        const rUpper = (state.maxMeasuredVoltage.value - state.adcReferenceVoltage.value) * state.lowerResistor.value / state.adcReferenceVoltage.value;
        const inputImpedance = 1 / (1 / rUpper + 1 / state.lowerResistor.value);
        const power = state.maxMeasuredVoltage.value * state.maxMeasuredVoltage.value / (state.lowerResistor.value + rUpper);

        const errorFraction = Math.exp(-state.adcSamplingCycles / (state.adcFrequency.value * state.adcCapacitance.value * (inputImpedance + state.adcSamplingSwitchResistance.value)));
        const precisionBits = -Math.log2(errorFraction);
        const errorVoltage = state.adcReferenceVoltage.value * errorFraction;

        //const maxAdcExternalInputImpedance = (state.adcSamplingCycles - 0.5) / (state.adcFrequency.value * state.adcCapacitance.value * Math.log(Math.pow(2, state.adcResolution + 2))) - state.adcSamplingSwitchResistance.value;

        return <div className="toolContainer">
            <div className="row">
                <FrequencyInput stateAccess={stateAccess} k="switchingFrequency" className="col" />
                <FrequencyInput stateAccess={stateAccess} k="adcFrequency" className="col" />
            </div>
            <div className="row">
                <NumberInput stateAccess={stateAccess} k="adcSamplingCycles" className="col" />
                <NumberInput stateAccess={stateAccess} k="adcResolution" className="col" />
            </div>
            <div className="row">
                <CapacitanceInput stateAccess={stateAccess} k="adcCapacitance" className="col" label="ADC Capacitance" />
                <ResistanceInput stateAccess={stateAccess} k="adcSamplingSwitchResistance" className="col" label="ADC Sampling Resistance" />
            </div>
            <div className="row">
                <VoltageInput stateAccess={stateAccess} k="maxMeasuredVoltage" className="col" />
                <VoltageInput stateAccess={stateAccess} k="adcReferenceVoltage" className="col" />
                <ResistanceInput stateAccess={stateAccess} k="lowerResistor" className="col" />
            </div>

            <div>
                ADC Cycles per Measurement (Sample and Conversion): {round(adcCyclesPerMeasurement, 0)}<br />
                ADC Cycles per Switching Period: {round(adcCyclesPerPeriod, 0)}
                <RangeIndicator value={adcCyclesPerMeasurement} min={0} max={adcCyclesPerPeriod - 2} format={x => '' + round(x, 0)}
                    maxWarning="Leave at least some ADC cycles for triggering delays" />
            </div>

            <div>
                Upper Resistor: {formatValue(rUpper, resistanceUnits)}<br />
                Output Impedance: {formatValue(inputImpedance, resistanceUnits)} <br />
                Error Voltage:  {formatValue(errorVoltage, voltageUnits)} <br />
                Precision [Bit]:  {round(precisionBits, 0)} <br />
                <RangeIndicator value={errorVoltage} min={0} max={state.adcReferenceVoltage.value / Math.pow(2, state.adcResolution)} units={voltageUnits}
                    maxWarning="Try to use smaller resistors" />
            </div>
            <div>
                Power: {formatValue(power, powerUnits)}
                <RangeIndicator value={power} min={0} max={50e-3} maxWarning="You are loosing quite some power in the resistors. Try using larger ones." units={powerUnits} />
            </div>
            <div>
                <button type="button" className="btn btn-secondary" onClick={stateAccess[2]}>Reset</button>
            </div >
        </div >
    });
}