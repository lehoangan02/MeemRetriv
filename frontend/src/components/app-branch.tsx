import LogoIcon from "@/components/icons/logo-icon";
import { APP_NAME } from "@/config/app";

export default function AppBranch() {
  return (
    <div className="flex items-center gap-3">
      <LogoIcon className="size-12" />
      <p className="font-mystery-quest text-3xl leading-none">{APP_NAME}</p>
    </div>
  );
}
