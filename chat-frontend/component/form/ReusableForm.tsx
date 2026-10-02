"use client";
import React from 'react'
import { FormStyles } from '../enums/FormStyles';
import { Formik, Form, FormikHelpers, FormikValues } from 'formik';
import { InputFieldProps } from '../ui/input/input.types';
import { FormButton } from '../enums/ButtonStyles';
import InputField from '../ui/input/InputField';
import { FieldStyles } from '../enums/FieldStyles';
import type { AnyObjectSchema } from 'yup';

interface AuthFormProps<T extends FormikValues> {
  enableReinitialize?: boolean;
  initialValues: T;
  validationSchema: AnyObjectSchema;
  onSubmit: (values: T, action: FormikHelpers<T>) => void | Promise<void>;
  fields: InputFieldProps[];
  formTitle?: string;
  formSubtitle?: React.ReactNode;
  buttonText?: string;
  buttonClassName?: string;
  formStyle?: FormStyles | string;
  fieldStyles?: FieldStyles | string;
  // false -> Render Inline (Inside A Modal / Card) Instead Of Centering On A Full Screen
  fullScreen?: boolean;
  // Extra Content Under The Button (Links Like "Already Have An Account?")
  footer?: React.ReactNode;
}

const ReusableForm = <T extends FormikValues>({
  enableReinitialize,
  initialValues,
  validationSchema,
  onSubmit,
  fields,
  formTitle = "Fill The Form",
  formSubtitle,
  buttonText ="Submit The Form",
  buttonClassName = FormButton.PRIMARY,
  formStyle = '',
  fieldStyles,
  fullScreen = true,
  footer,
}: AuthFormProps<T>) => {
  const form = (isSubmitting: boolean) => (
    <Form className={`${formStyle} space-y-6`} noValidate>
      {formTitle && (
        <div className='text-center mb-8'>
          <h2 className='font-display text-3xl font-bold text-white mb-2 tracking-tight'>{formTitle}</h2>
          {formSubtitle && <p className='text-sm text-muted'>{formSubtitle}</p>}
          <div className='w-16 h-1 bg-gradient-to-r from-cyan-400 to-violet-400 rounded-full mx-auto mt-4'></div>
        </div>
      )}
      <div>
        {fields.map((f) => (
          <InputField key={f.name} variant={fieldStyles}  {...f} />
        ))}
      </div>
      <div className='pt-2'>
        <button
          type='submit'
          disabled={isSubmitting}
          className={`${isSubmitting ? FormButton.DISABLED : buttonClassName} w-full transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98]
            ${isSubmitting ? '' : 'cursor-pointer'}`}
        >
          {
            isSubmitting ? (
              <div className='flex items-center justify-center space-x-2'>
                <div className='w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin'></div>
                <span>Submitting...</span>
              </div>
            ):
            (
              buttonText
            )
          }
        </button>
      </div>
      {footer}
    </Form>
  );

  return (
      <Formik
        enableReinitialize={enableReinitialize || false}
        initialValues={initialValues}
        validationSchema={validationSchema}
        onSubmit={onSubmit}
      >
        {({ isSubmitting }) => fullScreen ? (
          <div className='w-full min-h-screen flex justify-center items-center px-4'>
            {form(isSubmitting)}
          </div>
        ) : form(isSubmitting)}
      </Formik>
  )
}

export default ReusableForm
