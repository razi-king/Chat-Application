"use client";
import React, { useEffect, useState } from 'react'
import { InputFieldProps } from './input.types'
import { FieldStyles } from '@/component/enums/FieldStyles'
import { useField } from 'formik'
import { Eye, EyeOff, Upload } from 'lucide-react'
const InputField = ({
    name,
    label,
    type = "text",
    placeHolder,
    disabled,
    readOnly,
    className = '',
    options = [],
    rows = 4,
    variant = FieldStyles.DEFAULT,
    value,
    accept,
}: InputFieldProps) => {
    const [field, meta, helpers] = useField(name);
    const [showPassword, setShowPassword] = useState<boolean>(false);
    const hasErrors = meta.touched && meta.error ? true : false;
    const [isFocused, setIsFocused] = useState<boolean>(false);
    useEffect(() => {
        // Only override value if explicitly provided prop (not for internal formik state)
        if(value !== undefined){
            helpers.setValue(value);
        }
    }, [value]);
    const isFilled = field.value && (
        (typeof field.value === 'string' && field.value.length > 0) || 
        (field.value instanceof File)
    );
    const getInputClasses = () => {
        const baseClass = `w-full px-4 py-3 border-2 rounded-xl transition-all duration-200 focus:outline-none focus:ring-2 font-medium`
        if (hasErrors) {
            return `${baseClass} ${FieldStyles.ERROR} ${className}`.trim();
        }
        if (disabled) {
            return `${baseClass} ${FieldStyles.DISABLED} ${className}`.trim();
        }
        if (isFocused || isFilled) {
            return `${baseClass} ${FieldStyles.FOCUSED} ${className}`.trim();
        }
        return `${baseClass} ${variant || FieldStyles.DEFAULT} ${className}`.trim();
    }
    const inputClass = getInputClasses();

    let inputElement: React.ReactNode;
    switch(type){
        // --- NEW CASE FOR FILE UPLOAD ---
        case "file":
            inputElement = (
                <div className="relative">
                    <input
                        // NOTE: File inputs cannot be controlled via 'value' prop
                        // so we do NOT pass {...field} here.
                        name={field.name}
                        type="file"
                        accept={accept}
                        disabled={disabled}
                        className={`${inputClass} file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100 text-gray-500`}
                        onBlur={(e) => {
                            field.onBlur(e);
                            setIsFocused(false);
                        }}
                        onFocus={() => setIsFocused(true)}
                        onChange={(event) => {
                            // Manually set the file object into Formik state
                            const file = event.currentTarget.files ? event.currentTarget.files[0] : null;
                            helpers.setValue(file);
                        }}
                    />
                    {/* Optional: Visual indicator if file is selected */}
                    {!isFilled && !isFocused && (
                        <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
                            <Upload size={20} />
                        </div>
                    )}
                </div>
            );
            break;

        case "textarea":
            inputElement = (
                <textarea
                    {...field}
                    rows={rows}
                    placeholder={placeHolder}
                    disabled={disabled}
                    readOnly={readOnly}
                    className={inputClass}
                    onFocus={() => setIsFocused(true)}
                    onBlur={(e) => {
                        field.onBlur(e);
                        setIsFocused(false);
                    }}
                />
            );
            break;
            
        case "select":
            inputElement = (
                <select
                    {...field}
                    disabled={disabled}
                    className={inputClass}
                    onFocus={() => setIsFocused(true)}
                    onBlur={(e) => {
                        field.onBlur(e);
                        setIsFocused(false);
                    }}
                >
                    <option value="">{placeHolder || 'Select an option'}</option>
                    {options.map((opt) => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                </select>
            );
            break;
            
        case "password":
            inputElement = (
                <div className='relative'>
                    <input
                        {...field}
                        type={showPassword ? 'text' : 'password'}
                        placeholder={placeHolder}
                        disabled={disabled}
                        className={inputClass}
                        readOnly={readOnly}
                        onFocus={() => setIsFocused(true)}
                        onBlur={(e) => {
                            field.onBlur(e);
                            setIsFocused(false);
                        }}
                    />
                    <button
                        type='button'
                        onClick={() => setShowPassword((prev) => !prev)}
                        className='absolute right-4 top-1/2 -translate-y-1/2 text-primary-500 hover:text-primary-700 transition-colors duration-200'
                        tabIndex={-1}
                    >
                        {showPassword ? <EyeOff size={20}/> : <Eye size={20}/>}
                    </button>
                </div>
            );
            break;
            
        default:
            inputElement = (
                <input
                    {...field}
                    type={type}
                    placeholder={placeHolder}
                    readOnly={readOnly}
                    disabled={disabled}
                    className={inputClass}
                    onFocus={() => setIsFocused(true)}
                    onBlur={(e) => {
                        field.onBlur(e);
                        setIsFocused(false);
                    }}
                />
            );
    }
  return (

    <div className='mb-6'>
            {label && (
                <label 
                    htmlFor={name}
                    className={`block mb-3 font-semibold text-sm transition-colors duration-200 ${
                        hasErrors ? 'text-red-600' : 
                        isFocused ? 'text-primary-700' : 
                        'text-primary-800'
                    }`}
                >
                    {label}
                    {hasErrors && <span className="text-red-500 ml-1">*</span>}
                </label>
            )}
            {inputElement}
            {meta.touched && meta.error && (
                <div className="flex items-center space-x-1 text-red-600 text-sm mt-2">
                    <svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                    </svg>
                    <span>{meta.error}</span>
                </div> 
            )}
        </div>
  )
}
export default InputField