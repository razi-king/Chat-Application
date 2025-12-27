import React from 'react'
import { FormStyles } from '../enums/FormStyles';
import { Formik, Form } from 'formik';
import { InputFieldProps } from '../ui/input/input.types';
import { FormButton } from '../enums/ButtonStyles';
import InputField from '../ui/input/InputField';
import { FieldStyles } from '../enums/FieldStyles';

interface AuthFormProps {
  enableReinitialize?: boolean;
  initialValues: Record<string, any>;
  validationSchema: any;
  onSubmit: (values: any, action: any) => void;
  fields: InputFieldProps[];
  formTitle?: string;
  buttonText?: string;
  buttonClassName?: string;
  formStyle?: FormStyles | string;
  fieldStyles?: FieldStyles | string;
}

const ReusableForm = ({
  enableReinitialize,
  initialValues,
  validationSchema,
  onSubmit,
  fields,
  formTitle = "Fill The Form",
  buttonText ="Submit The Form",
  buttonClassName = FormButton.PRIMARY,
  formStyle = '',
  fieldStyles,

}: AuthFormProps) => {
  return (
      <Formik
        enableReinitialize={enableReinitialize || false}
        initialValues={initialValues}
        validationSchema={validationSchema}
        onSubmit={onSubmit}
      >
        {({ isSubmitting }) => (
          <div className='w-full min-h-screen flex justify-center items-center'>
            <Form className={`${formStyle} space-y-6`}>
              <div className='text-center mb-8'>
                <h2 className='text-2xl font-bold text-primary-900 mb-2'>{formTitle}</h2>
                <div className='w-16 h-1 bg-green-500 rounded-full mx-auto'></div>
              </div>
              <div>
                {fields.map((f) => (
                  <InputField key={f.name} variant={fieldStyles}  {...f} />
                ))}
              </div>
              <div className='pt-4'>
                <button
                  type='submit'
                  disabled={isSubmitting}
                  className={`${buttonClassName} w-full transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98]
                    ${isSubmitting ? FormButton.DISABLED: 'cursor-pointer'}`}
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
            </Form>
          </div>
        )}
      </Formik>
  )
}

export default ReusableForm