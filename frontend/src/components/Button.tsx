import type { ComponentProps } from 'react'
import { LoaderCircle } from 'lucide-react'
import { buttonClass, type ButtonSize, type ButtonVariant } from './buttonStyles'

type ButtonProps = ComponentProps<'button'> & {
  variant?: ButtonVariant
  size?: ButtonSize
  /** Stretch to the container width (forms); false sizes the button to its label */
  fullWidth?: boolean
  isLoading?: boolean
}

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = true,
  isLoading = false,
  disabled,
  className,
  children,
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
      className={`${buttonClass(variant, size, fullWidth)} ${className ?? ''}`}
      {...props}
    >
      {/* Loading keeps the label (UI_GUIDE 7.1): a 14px spinner on the left, button disabled */}
      {isLoading && <LoaderCircle aria-hidden className="size-3.5 shrink-0 animate-spin" />}
      {children}
    </button>
  )
}
