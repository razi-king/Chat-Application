import { FieldStyles } from "@/component/enums/FieldStyles";

interface OptionType {
    label: string;
    value: string | number;
}
export interface InputFieldProps {
    name: string;
    label?: string;
    type?: string;
    placeHolder?: string;
    disabled?: boolean;
    readOnly?: boolean;
    className?: string; 
    options?: OptionType[];
    rows?: number;
    variant?: FieldStyles | string;
    value?: string | number;
    accept?: string; // Use To Specify File Types
}