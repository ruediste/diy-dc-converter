import React, { ReactNode, useEffect } from "react";
import { Dispatch, SetStateAction, useState } from "react";

// Hook
export default function useLocalStorage<T>(key: string, initialValue: T): [T, Dispatch<SetStateAction<T>>, () => void] {
    // State to store our value
    // Pass initial state function to useState so logic is only executed once
    const [storedValue, setStoredValue] = useState<T>(() => {
        if (typeof window === "undefined") {
            return initialValue;
        }
        try {
            // Get from local storage by key
            const item = window.localStorage.getItem(key);
            // Parse stored json or if none return initialValue
            return item ? JSON.parse(item) : initialValue;
        } catch (error) {
            // If error also return initialValue
            console.log(error);
            return initialValue;
        }
    });
    // Return a wrapped version of useState's setter function that ...
    // ... persists the new value to localStorage.
    const setValue: Dispatch<SetStateAction<T>> = (value) => {
        try {
            // Allow value to be a function so we have same API as useState
            const valueToStore =
                value instanceof Function ? value(storedValue) : value;
            // Save state
            setStoredValue(valueToStore);
            // Save to local storage
            if (typeof window !== "undefined") {
                window.localStorage.setItem(key, JSON.stringify(valueToStore));
            }
        } catch (error) {
            // A more advanced implementation would handle the error case
            console.log(error);
        }
    };
    return [storedValue, setValue, () => setValue(initialValue)];
}


class ErrorBoundary extends React.Component<{ children: (hasError: boolean) => ReactNode }, { hasError: boolean }> {
    constructor(props: any) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError(error: Error) {
        return { hasError: true };
    }

    render() {
        return this.props.children(this.state.hasError);
    }
}

function UseMapLocalStorageHelper<T>(props: { hasError: boolean, initialValue: T, access: [T, Dispatch<SetStateAction<T>>, () => void], map: (access: [T, Dispatch<SetStateAction<T>>, () => void]) => JSX.Element }) {
    useEffect(() => {
        if (props.hasError) {
            props.access[2]();
        }
    });
    return props.map([props.hasError ? props.initialValue : props.access[0], props.access[1], props.access[2]]);
}


export function useMapLocalStorage<T>(key: string, initialValue: T, map: (access: [T, Dispatch<SetStateAction<T>>, () => void]) => JSX.Element): JSX.Element {
    const access = useLocalStorage(key, initialValue);
    const [resetCount, setResetCount] = useState(0);
    return <ErrorBoundary key={resetCount}>
        {hasError => < UseMapLocalStorageHelper access={
            [access[0], access[1], () => {
                setResetCount(i => i + 1);
                access[2]();
            }]} hasError={hasError} map={map} initialValue={initialValue} />}
    </ErrorBoundary>
}