import { Link, type LinkProps } from 'react-router-dom'
import { buttonClass, type ButtonSize, type ButtonVariant } from './buttonStyles'

type LinkButtonProps = LinkProps & {
  variant?: ButtonVariant
  size?: ButtonSize
}

/** Navigation that looks like a button ("Tạo chuyến đi"): a real link, so it opens in a new tab and reads as one. */
export function LinkButton({ variant = 'primary', size = 'md', className, ...props }: LinkButtonProps) {
  return <Link className={`${buttonClass(variant, size, false)} ${className ?? ''}`} {...props} />
}
