import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useState } from 'react'
import { Link } from '@tanstack/react-router'
import { toast } from 'sonner'
import { authApi } from '@/shared/api'

const registerSchema = z.object({
  businessName: z.string().min(2, 'Business name must be at least 2 characters'),
  ownerName: z.string().min(2, 'Your name must be at least 2 characters'),
  mobile: z.string().regex(/^[6-9]\d{9}$/, 'Enter a valid 10-digit Indian mobile number'),
  email: z.string().email('Enter a valid email address'),
  password: z.string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[0-9]/, 'Password must contain at least one number'),
})

type RegisterForm = z.infer<typeof registerSchema>

export default function RegisterPage() {
  const [step, setStep] = useState<'form' | 'otp'>('form')
  const [mobile, setMobile] = useState('')
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [registrationDone, setRegistrationDone] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = async (data: RegisterForm) => {
    setLoading(true)
    try {
      await authApi.register(data)
      setMobile(data.mobile)
      setStep('otp')
      toast.success('OTP sent to your mobile number!')
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const verifyOtp = async () => {
    if (otp.length !== 6) {
      toast.error('Please enter a 6-digit OTP')
      return
    }
    setLoading(true)
    try {
      await authApi.verifyOtp(mobile, otp)
      toast.success('Mobile verified! Setting up your account...')
      window.location.href = '/onboarding'
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Invalid OTP. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const resendOtp = async () => {
    try {
      await authApi.resendOtp(mobile)
      toast.success('New OTP sent!')
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Could not resend OTP. Please wait before retrying.')
    }
  }

  if (step === 'otp') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 p-4">
        <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-6">
            <div className="inline-flex items-center justify-center w-14 h-14 bg-green-100 rounded-full mb-3">
              <span className="text-green-600 text-2xl">📱</span>
            </div>
            <h2 className="text-2xl font-bold text-gray-900">Verify your mobile</h2>
            <p className="text-gray-500 mt-1">
              We sent a 6-digit code to{' '}
              <span className="font-medium text-gray-700">
                {mobile.substring(0, 2)}****{mobile.slice(-2)}
              </span>
            </p>
          </div>

          <div className="space-y-4">
            <input
              type="text"
              inputMode="numeric"
              maxLength={6}
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
              placeholder="123456"
              className="w-full px-4 py-3 text-center text-2xl tracking-widest border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <button
              onClick={verifyOtp}
              disabled={loading || otp.length !== 6}
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium rounded-lg transition-colors"
            >
              {loading ? 'Verifying...' : 'Verify OTP'}
            </button>

            <button
              onClick={resendOtp}
              className="w-full py-2 text-sm text-gray-500 hover:text-blue-600"
            >
              Didn't receive? Resend OTP
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-600 rounded-2xl mb-4 shadow-lg">
            <span className="text-white font-bold text-2xl">QS</span>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Start your free trial</h1>
          <p className="text-gray-500 mt-1">14 days free — no credit card needed</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {[
              { field: 'businessName' as const, label: 'Business Name', placeholder: 'Sharma Cafe' },
              { field: 'ownerName'    as const, label: 'Your Name',     placeholder: 'Rajesh Sharma' },
              { field: 'mobile'       as const, label: 'Mobile Number', placeholder: '9876543210', type: 'tel' },
              { field: 'email'        as const, label: 'Email',         placeholder: 'rajesh@cafe.com', type: 'email' },
              { field: 'password'     as const, label: 'Password',      placeholder: '••••••••', type: 'password' },
            ].map(({ field, label, placeholder, type = 'text' }) => (
              <div key={field}>
                <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                <input
                  {...register(field)}
                  type={type}
                  placeholder={placeholder}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                {errors[field] && (
                  <p className="mt-1 text-sm text-red-600">{errors[field]?.message}</p>
                )}
              </div>
            ))}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium rounded-lg transition-colors mt-2"
            >
              {loading ? 'Creating account...' : 'Create free account →'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-600 hover:text-blue-800 font-medium">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
